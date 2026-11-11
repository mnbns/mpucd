# Idempotent Background Transaction Processing Service

FDE L2 Practical Assessment. A background console service that processes
banking transaction requests (CREDIT/DEBIT/REVERSAL) idempotently, in the
face of duplicates, out-of-order arrival, and transient failures.

- **Design note**: [`DESIGN.md`](DESIGN.md) — User Stories, Acceptance
  Criteria, HLD, LLD, flow charts, sequence diagram, design decisions,
  traceability matrix, known gaps, AI-usage notes.
- **Live, clickable version of the same design** (bidirectional links
  between every requirement and every design artifact): **[live docs
  site](https://mnbns.github.io/mpucd/)**.
- **Release notes**: [`RELEASE_NOTES.md`](RELEASE_NOTES.md) — defects found
  and fixed, final test results.
- **Test report**: [`docs/test-report/ExtentReport.html`](docs/test-report/ExtentReport.html)

## Requirements

- JDK 17
- Maven 3.9+ (or use the included config pointing at a local install — see below)

## Build & run

```bash
cd transaction-service
mvn spring-boot:run
```

This loads the sample batch (`src/main/resources/sample-transactions.json`,
the problem statement's §9.8 test data) and processes it in order. Expected
console output:

```
RECEIVED   REQ-2      TXN-2002  seq=2  ACC-201  -> PENDING_ORDER (waiting for an earlier sequence)
RECEIVED   REQ-1      TXN-2001  seq=1  ACC-201  -> PROCESSED
           REQ-2      TXN-2002  seq=2  ACC-201  -> PROCESSED (released once seq=1 resolved)
           REQ-2-DUP  TXN-2002  seq=2  ACC-201  -> DUPLICATE
RECEIVED   REQ-3      TXN-2003  seq=3  ACC-201  -> PROCESSED
RECEIVED   REQ-4      TXN-2004  seq=4  ACC-202  -> FAILED (amount must be positive)
RECEIVED   REQ-5      TXN-2005  seq=5  ACC-203  -> RETRY_PENDING, then PROCESSED (attempt 2)
---
SUMMARY: PROCESSED=4  DUPLICATE=1  FAILED=1  RETRY_PENDING=0  PENDING_ORDER=0
---
BALANCE ACC-201 = 85
BALANCE ACC-202 = 0
BALANCE ACC-203 = 500
```

## Run the tests

```bash
cd transaction-service
mvn test
```

Runs 11 Cucumber feature files (one per User Story, under
`src/test/resources/features/`) — 27 scenarios, 104 steps, all automated
against the real engine classes (no mocks, no HTTP). Produces an HTML report
at `target/cucumber-reports/ExtentReport.html` (a copy of the last known-good
run is committed at [`docs/test-report/ExtentReport.html`](docs/test-report/ExtentReport.html)).

## Project layout

```
transaction-service/
  src/main/java/.../model/      TransactionRequest, TransactionStatus, TransactionType
  src/main/java/.../input/      TransactionSource (interface) + InMemoryTransactionSource
  src/main/java/.../store/      IdempotencyStore (interface), InMemoryIdempotencyStore, AccountLedger
  src/main/java/.../ordering/   SequenceBuffer (Option A: sequence-based out-of-order handling)
  src/main/java/.../retry/      RetryPolicy, TransientProcessingException
  src/main/java/.../service/    Validator, TransactionProcessor (orchestrator), ProcessingSummary
  src/test/resources/features/  11 Cucumber .feature files, one per User Story
  src/test/java/.../steps/      Step definitions, one file per User Story
docs/                            Design review app (Problem Statement + Design tabs) - also the GitHub Pages site
DESIGN.md                        Design note (source of truth, also rendered live in docs/)
RELEASE_NOTES.md                 Defects found & fixed, final test results
DEMO_SCRIPT.md                   Narration + command sequence for a recorded walkthrough
```

## Git history

This repo's commit history is organized to show the actual build process,
not just the finished result: `design` → `feature/00-basic-setup` → nine
feature branches (one per User Story, US-02 through US-10) → `release/1.0`
→ merged to `main` as `v1.0`. Run `git log --oneline --graph --all` to see it.

## License

[GNU GPLv3](LICENSE)
