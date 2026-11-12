# docs/ — live, clickable design & problem-statement viewer

## Why this is here

Everything the assessment actually needs lives in `DESIGN.md` at the repo
root — this folder is a convenience layer on top of it, not a separate
source of truth. `DESIGN.md` and the original problem statement are both
long, ID-heavy documents (`L2-FR-xx`, `NFR-xx`, `US-xx`, `AC-xx.y`, `DD-xx`)
that are tedious to cross-reference by scrolling and searching. This is a
small static web app that renders both side by side with the IDs turned
into clickable links, so you can jump from a requirement straight to the
User Story/design decision that implements it, and back.

## What's in here

- **`index.html` / `app.js` / `style.css`** — a single-page AngularJS shell
  with two tabs: "Problem Statement" and "Design & Traceability".
- **`data.js`** — the original problem statement, structured as a section
  tree (feeds the "Problem Statement" tab).
- **`design-data.js`** — `DESIGN.md`'s content restructured as data:
  requirements, User Stories/ACs, design decisions, the traceability
  matrix — each tagged with the section anchor it links back to (feeds the
  "Design & Traceability" tab, and the bidirectional links between the two
  tabs).
- **`server.js` / `package.json`** — a two-line Express static file
  server, only there so the page has a stable `http://localhost` origin
  to run from; it serves no API, no backend logic.
- **`test-report/ExtentReport.html`** — a committed snapshot of the last
  known-good Cucumber test report, linked from the main `README.md`.
- **`node_modules/`** — Express's own dependencies, installed by `npm
  install`, not committed (see `.gitignore`).

## How to run it

**Easiest — no install needed:** just open `docs/index.html` directly in a
browser (double-click it, or `start docs\index.html` on Windows). Angular,
Bootstrap, and Mermaid load from a CDN; `data.js`/`design-data.js`/`app.js`
load as plain `<script>` tags, which works fine straight off disk.

**Alternative — via the bundled Express server**, if you want it served
from `http://localhost` instead of a `file://` URL:

```bash
cd docs
npm install
npm start
```

Then open **http://localhost:3000** in a browser. Stop it with `Ctrl+C`.
