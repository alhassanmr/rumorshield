# RumorShield Backend

Spring Boot 3.3 / Java 21 / PostgreSQL. Single service, no external API
dependency — the entire verification pipeline is deterministic Java.

## How the pipeline works

```
POST /api/v1/claims/verify  { "claimText": "..." }
        │
        ▼
  InternalReasoningEngine.extractClaim   <- rule-based (keyword + regex), no external call
        │
        ▼
  ClaimMatchingService.match             <- decides the verdict, deterministically
        │
        ▼
  InternalReasoningEngine.explainVerdict <- template-based, filled from matched record data
        │
        ▼
  Persisted to Postgres (verification_results table)
        │
        ▼
  VerifyResponse (verdict, evidence, sources, recommendation)
```

No LLM, no API key, no network call at request time, works fully offline
once Postgres is up. This is a genuine trade-off — see the top of
`InternalReasoningEngine.java` for what it costs vs. an LLM-based version
(mainly: extraction only recognizes vocabulary it has rules for, and
explanations are templates rather than free-form prose). Point judges at
`ClaimMatchingService.java` + `InternalReasoningEngine.java` together if
asked how verdicts are decided — nothing in either file is a model call.

## Setup

1. Requires Java 21, Maven, and Docker (for local Postgres).
2. Start Postgres:
   ```bash
   docker compose up -d
   ```
   This starts a `rumorshield` database on `localhost:5432` with default
   credentials matching `application.properties` (override via `DB_URL`,
   `DB_USER`, `DB_PASSWORD` env vars if you're pointing at something else).
   Tables are created automatically on first run (`ddl-auto=update`).
3. Run:
   ```bash
   mvn spring-boot:run
   ```
   No environment variables required — there's no external API to configure.
4. Test:
   ```bash
   curl -X POST http://localhost:8080/api/v1/claims/verify \
     -H "Content-Type: application/json" \
     -d '{"claimText": "They are saying Ghana Card renewal now costs GHS 200"}'
   ```

   The response includes a `claimId` — fetch it again anytime with:
   ```bash
   curl http://localhost:8080/api/v1/claims/{claimId}
   ```

   Try these to exercise each verdict:
   - VERIFIED-ish: "Ghana Card renewal costs GHS 150"
   - CONTRADICTED: "President Mahama is giving out GHS 1500 development cash grant"
   - CONTRADICTED (fee mismatch): "Standard passport now costs GHS 500"
   - OUTDATED: "NHIS registration is free right now"
   - CONTRADICTED (police): "Someone offered me a guaranteed police recruitment slot for a fee"
   - UNVERIFIED, topic recognized: "Ghana Police is closing all stations next week" — no matching
     record, but topic "Ghana Police" is recognized, so the response points to police.gov.gh
   - UNVERIFIED, topic unrecognized: any claim about a topic not in the dataset at all — honest
     "outside what we've verified" response, no fabricated source

## Current demo scope: WEB ONLY

WhatsApp and USSD are intentionally deferred — not built, not simulated,
not referenced in the live demo. Everything in this codebase right now
supports the web flow end-to-end (React frontend → this API → Postgres).

## Known scope cuts (deliberate, for the sprint)

- Matching is keyword/rule-based over a small curated JSON dataset — not a
  vector DB or live web search. This is intentional; see the product spec.
- Claim extraction and explanation are rule-based (`InternalReasoningEngine`),
  not LLM-based — a deliberate trade-off for zero cost / zero dependency /
  full offline operation, at the cost of handling only vocabulary the
  dataset already knows about. See the class-level comment for the honest
  pros/cons if this comes up in Q&A.
- Persistence is a single flat `verification_results` table, not the fully
  normalized Claim/Source/Evidence model the spec describes — that level of
  structure only pays for itself once a claim can match multiple ranked
  evidence items, which is out of scope here.
- No auth on the endpoint — fine for a hackathon demo, not for production.

## What happens when a claim isn't in the dataset

This matters enough to call out on its own. Three distinct outcomes, not one flat "unverified":

1. **Topic and claim both match a record** → VERIFIED / CONTRADICTED / OUTDATED, with the real source.
2. **Topic recognized, but no record matches the specific claim** → UNVERIFIED, but the response
   still names the topic and points to the real official portal for it (`TOPIC_PORTALS` map in
   `InternalReasoningEngine.java`) — e.g. an unrecognized Ghana Police claim still gets pointed to
   police.gov.gh, even with no exact match.
3. **Topic itself isn't recognized at all** → UNVERIFIED, honest "outside what we've verified"
   response. No fabricated or guessed source — better to say "we don't cover this yet" than to
   point somewhere we're not actually confident is right.

This is the practical answer to "what if someone searches something we haven't curated" — it
degrades gracefully by topic-area rather than by individual claim.

## Next: extend the dataset

Add more records to `src/main/resources/claims-dataset.json` in the same
shape. `InternalReasoningEngine` pulls its recognized topics directly from
this dataset, so adding a new topic there also extends what the engine can
recognize — no code change needed for that part. If a new claim's phrasing
doesn't match well, check `SUBJECT_KEYWORDS` in `InternalReasoningEngine.java`
— that list may need a new keyword.
