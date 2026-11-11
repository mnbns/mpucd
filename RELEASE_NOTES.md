# Release Notes — v1.0

## Summary

9 User Stories (US-02 through US-10) automated iteratively on top of the
Basic Setup skeleton (US-01 input layer + US-11 test infrastructure). Final
state: **27/27 Cucumber scenarios passing, 104/104 steps passing**, verified
against the full §9.8 sample batch end to end, including the banking ledger
balances (`ACC-201=85, ACC-202=0, ACC-203=500`) — proof idempotency actually
held, not just that statuses looked right.

## Defects found and fixed during this build

All four were found by actually running the code against real data or
running the real test suite — not by inspection.

| ID | Found during | Summary | Status |
|---|---|---|---|
| DEFECT-01 | Automating US-05 (out-of-order) | Per-account sequence cursor stranded `ACC-203`'s only transaction forever — it could never become "ready", so it never reached the retry logic, contradicting §9.8's own expected outcome. Fixed: switched `SequenceBuffer` to a single batch-wide cursor, advanced on every resolved sequence number including validation failures. Recorded as design decision DD-02. | **Fixed** |
| DEFECT-02 | Same pass, at the US-04/US-05 boundary | The stale-sequence branch was drawn in the FC-03 flowchart but never implemented — `REQ-2-DUP` came out `PENDING_ORDER` instead of `DUPLICATE`. Fixed by adding the `isAlreadyResolved` check. | **Fixed** |
| DEFECT-03 | Wiring the Cucumber runner (US-11 infra) | Surefire silently ran 0 tests — the runner class was named `CucumberTestRunner`, which doesn't match Surefire's default `*Test`/`*Tests` include pattern, so nothing ran and nothing failed. Renamed to `RunCucumberTest`. | **Fixed** |
| DEFECT-04 | Automating US-02/US-03 | Step-text collision: both features used the phrase "it is ingested" with different intended meaning. Cucumber matches by text globally, not per file, so US-03's lifecycle check was silently running the full processing pipeline instead of checking the just-constructed state. Reworded the US-03 step. | **Fixed** |
| DEFECT-05 | Cross-checking the design against an independent AI analysis of the same assessment | Idempotency was keyed only on `requestId`, relying on the sequence-staleness check (DEFECT-02's fix) to incidentally catch `REQ-2-DUP`. That's fragile: a duplicate reusing a fresh, never-seen sequence number would have slipped through. Fixed: `IdempotencyStore` is now dual-key (`requestId` + `transactionId`). Recorded as design decision DD-03. | **Fixed** |

## Known gaps (not fixed — documented, per §9.7)

See `DESIGN.md` → "Known Gaps (Production Hardening, Extended)" for the full
list: stuck-pending TTL, atomic-claim concurrency vs. the narrower
ledger-only concurrency test actually implemented, permutation/invariant
testing as a stronger oracle, idempotency key retention.

## Test evidence

- `mvn test` → `Tests run: 27, Failures: 0, Errors: 0, Skipped: 0`
- HTML report: [`docs/test-report/ExtentReport.html`](docs/test-report/ExtentReport.html)
- Sample run output: see `README.md`

## What's in this release

- Full engine: `TransactionProcessor` orchestrating `Validator` →
  `IdempotencyStore` → `SequenceBuffer` → `RetryPolicy` → `AccountLedger`.
- 11 Cucumber feature files (one per User Story), 27 scenarios, fully automated.
- Design note with HLD, LLD, 4 flow charts, 1 sequence diagram, 6 design
  decisions, traceability matrix — also rendered live in `docs/`.
- This release notes file and a demo script (`DEMO_SCRIPT.md`) for a recorded walkthrough.
