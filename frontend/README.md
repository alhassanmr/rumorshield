# RumorShield Frontend

Vite + React, no framework beyond that — kept deliberately light for the
hackathon timeline. Plain CSS with variables in `src/App.css` rather than
a UI library, so the visual identity is fully custom.

## Design intent

Not a chat interface and not a generic SaaS dashboard. The claim box reads
as an intake form; the result reads as an evidence report — a small seal
mark, a claim ID, source tags by type (official / news / factcheck), and a
verdict-colored left border, closer to a document than a card in a feed.

## Setup

```bash
npm install
npm run dev
```

Runs on http://localhost:5173 and proxies `/api` to the backend on
`localhost:8080` (see `vite.config.js`). Start the backend first.

## Where your own code goes

- `src/components/` — add new UI pieces here (e.g. the USSD simulator for Phase 4)
- `src/App.css` — all styling lives here; verdict colors are CSS variables
  at the top (`--verified`, `--contradicted`, `--outdated`, `--unverified`)
  if you want to retheme
- `src/api.js` — add new backend calls here as you add endpoints
