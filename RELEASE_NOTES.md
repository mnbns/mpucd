# Release Notes — v1.1

## Summary

12 User Stories (US-01 through US-12) automated — the original 11-story
scope plus a 12th, added after re-reading the assessment checklist and
finding "background processing component" named as an item distinct from
"transaction request ingestion." Final state, re-verified from a clean
build: **39/39 Cucumber scenarios passing, 160/160 steps passing**, plus
**103/103 JUnit unit tests passing** — **142 tests total, 0 failures, 0
errors** — verified against the full §9.8 sample batch end to end,
including the banking ledger balances (`ACC-201=85, ACC-202=0,
ACC-203=500`) — proof idempotency actually held, not just that statuses
looked right. A fresh, independent run of the live application (not just
the test suite) was also cross-checked line-by-line against the same
expected outcome; see `Traceability Matrix After.html` and `Execution
Instructions and Sample IO.md` for that evidence.

## Design

Full HLD/LLD, 4 flow charts, 1 sequence diagram, and 12 design decisions
(DD-01 through DD-12) in `DESIGN.md`, including two decisions (DD-02, DD-03)
that came directly out of defects found while automating US-05 and
cross-checking the design (see defect table below), and a 12th (DD-12) that
added the background worker component. `DESIGN.md`'s traceability matrix
maps every User Story/AC to its implementing class and scenario.

## What's Implemented

- Full engine: `TransactionProcessor` orchestrating `Validator` →
  `IdempotencyStore` → `SequenceBuffer` → `RetryPolicy` → `AccountLedger`,
  fed by a `BlockingQueue`-backed background worker (US-12/DD-12) instead
  of inline, synchronous processing.
- 12 Cucumber feature files (one per User Story), 39 scenarios, fully
  automated, plus 103 JUnit unit tests covering the engine classes
  individually.
- Design note with HLD, LLD, 4 flow charts, 1 sequence diagram, 12 design
  decisions, traceability matrix — also rendered live in `docs/`.
- This release notes file and a `deliverables/` folder with before/after
  traceability matrices, execution instructions with sample I/O, and raw
  unit-test evidence.

## % Complete

12/12 User Stories scoped for this release (US-01 through US-12) are
implemented and automated — **100% of v1.1's scope**. 39/39 Cucumber
scenarios, 160/160 steps, 103/103 unit tests passing (142 tests total).
32/33 Acceptance Criteria are automated (97.0%) — the one open item,
AC-11.2, is a self-imposed stretch criterion beyond the literal requirement
text, named explicitly rather than force-fit into a fake passing test (see
"Future Plans" below).

## Pending / Not Yet Implemented

None against v1.1's scope (US-01 through US-12) — fully delivered. Items
beyond that scope are tracked under "Future Plans" below, not here.

## Known Defects

None open at release time. All 6 defects found during the build (table
below) were fixed before release — see "Future Plans" for things that are
gaps by design, not bugs.

## Future Plans

Production-hardening items deliberately deferred past v1.1 (full detail in
`DESIGN.md` → "Known Gaps (Production Hardening, Extended)"): a TTL for
stuck-`PENDING` transactions, atomic-claim concurrency beyond the narrower
ledger-only concurrency test actually implemented, permutation/invariant
testing as a stronger correctness oracle, an idempotency-key retention
policy, and — new with US-12 — the background worker's queue is in-memory
and unbounded (no persistence or backpressure), and scaling it beyond one
consumer thread requires first moving the idempotency check to an atomic
store-level claim and partitioning the queue by `accountId`, neither of
which is implemented.

## Defects found and fixed during this build

All six were found by actually running the code against real data or
running the real test suite — not by inspection.

| ID | Found during | Summary | Status |
|---|---|---|---|
| DEFECT-01 | Automating US-05 (out-of-order) | Per-account sequence cursor stranded `ACC-203`'s only transaction forever — it could never become "ready", so it never reached the retry logic, contradicting §9.8's own expected outcome. Fixed: switched `SequenceBuffer` to a single batch-wide cursor, advanced on every resolved sequence number including validation failures. Recorded as design decision DD-02. | **Fixed** |
| DEFECT-02 | Same pass, at the US-04/US-05 boundary | The stale-sequence branch was drawn in the FC-03 flowchart but never implemented — `REQ-2-DUP` came out `PENDING_ORDER` instead of `DUPLICATE`. Fixed by adding the `isAlreadyResolved` check. | **Fixed** |
| DEFECT-03 | Wiring the Cucumber runner (US-11 infra) | Surefire silently ran 0 tests — the runner class was named `CucumberTestRunner`, which doesn't match Surefire's default `*Test`/`*Tests` include pattern, so nothing ran and nothing failed. Renamed to `RunCucumberTest`. | **Fixed** |
| DEFECT-04 | Automating US-02/US-03 | Step-text collision: both features used the phrase "it is ingested" with different intended meaning. Cucumber matches by text globally, not per file, so US-03's lifecycle check was silently running the full processing pipeline instead of checking the just-constructed state. Reworded the US-03 step. | **Fixed** |
| DEFECT-05 | Cross-checking the design against an independent AI analysis of the same assessment | Idempotency was keyed only on `requestId`, relying on the sequence-staleness check (DEFECT-02's fix) to incidentally catch `REQ-2-DUP`. That's fragile: a duplicate reusing a fresh, never-seen sequence number would have slipped through. Fixed: `IdempotencyStore` is now dual-key (`requestId` + `transactionId`). Recorded as design decision DD-03. | **Fixed** |
| DEFECT-06 | Syncing the live `docs/` site's data for US-12 | `docs/design-data.js` had a broken escape sequence (`\\'` instead of a proper escaped/curly apostrophe) in DD-02's rationale text — a JS syntax error that would have broken the entire live docs/ site (Design tab fails to render at all). Found via `node -c docs/design-data.js` failing. Fixed by replacing with a curly apostrophe, matching the style already used elsewhere in the same file; verified with `node -c` passing and a full module load. | **Fixed** |

## Test evidence

- `mvn test` → `39 Scenarios (39 passed)`, `160 Steps (160 passed)`, plus
  `103 JUnit unit tests (103 passed)` — `142 tests total, 0 failures, 0 errors`
- HTML report: [`Test Execution Report.html`](Test%20Execution%20Report.html)
  (also a committed snapshot at `docs/test-report/ExtentReport.html`)
- Raw unit-test evidence: [`Unit Test Execution Evidence.md`](Unit%20Test%20Execution%20Evidence.md)
- Before/after traceability: [`Traceability Matrix Before.html`](Traceability%20Matrix%20Before.html)
  / [`Traceability Matrix After.html`](Traceability%20Matrix%20After.html)
- Sample run output: see [`Execution Instructions and Sample IO.md`](Execution%20Instructions%20and%20Sample%20IO.md)
