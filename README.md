# RumorShield

Civic information verification assistant — built for the Andela × Open
Society Foundations invention sprint (Transparency & Accountability track).

**"Don't ask AI what to believe. Ask it to show you the evidence."**

## What it does

A person pastes a claim — a rumor, a WhatsApp forward, an official-sounding
notice — and RumorShield checks it against a curated set of verified
Ghanaian civic sources, then returns one of four verdicts (Verified /
Contradicted / Outdated / Unverified) with the actual evidence and a plain
next step. Claim extraction and explanation are rule-based, not an LLM call
— nothing in the pipeline is a model call, so the verdict is fully
deterministic and auditable. See `backend/.../ClaimMatchingService.java`
and `backend/.../InternalReasoningEngine.java`.

## Structure

```
rumorshield/
├── backend/    Spring Boot 3.3 API — verification pipeline, Postgres, curated dataset
└── frontend/   Vite + React UI — claim intake + evidence report
```

Each folder has its own README with setup steps. Quick start:

```bash
# terminal 1 — start Postgres
cd backend && docker compose up -d

# terminal 2 — backend
cd backend && mvn spring-boot:run

# terminal 3 — frontend
cd frontend && npm install && npm run dev
```

Then open http://localhost:5173.

## Status

**Current demo scope: web application only.** WhatsApp and USSD are
intentionally deferred — not built, not simulated — until the web flow is
solid end-to-end. Backend pipeline, Postgres persistence, and web UI are
built and working together. Not yet built: WhatsApp integration, USSD/SMS.
