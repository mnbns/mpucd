# US-12 — Decoupled Background Processing Worker

## Done
Implemented US-12 (traces to L2-FR-13, HLD-01, LLD-01) end to end:

- **Created** `transaction-service/src/main/java/com/mphasis/assessment/txservice/worker/BackgroundWorker.java`
  — the LLD-01 `BackgroundWorker` class: a `BlockingQueue<TransactionRequest>` (producer side,
  `submit()` enqueues and returns immediately) drained by a single dedicated consumer thread
  (a 1-thread `ExecutorService`, daemon so it never blocks JVM shutdown) that calls
  `TransactionProcessor.process(request)` per DD-12's prose. `awaitDrained()` blocks the caller
  until every request submitted so far has actually finished processing (wait/notify on a
  submitted-vs-processed counter pair), closing the "ingestion done" vs "processing done" race
  named in AC-12.2. A `consumerThreadCount()` getter (test-support only) records every distinct
  thread name observed draining the queue, to verify AC-12.3's "single sequential consumer"
  property behaviorally rather than via brittle reflection into JDK executor internals.
- **Changed** `transaction-service/src/main/java/com/mphasis/assessment/txservice/TransactionServiceApplication.java`
  — the `CommandLineRunner` now builds a `BackgroundWorker` around the `TransactionProcessor`,
  `start()`s it, submits the whole ingested batch to it, then `awaitDrained()`s before printing
  the summary/balances — replacing the old inline `for (request : batch) processor.process(request)`
  loop that gave ingestion and processing no architectural separation at all.
- **Changed** `transaction-service/src/test/java/com/mphasis/assessment/txservice/steps/ScenarioContext.java`
  — added a `public final BackgroundWorker worker` field wired around the scenario's existing
  `processor`, following the same eager-final-field pattern as the other shared components.
- **Created** `transaction-service/src/test/java/com/mphasis/assessment/txservice/steps/BackgroundWorkerSteps.java`
  — step definitions for the three ACs (see below). Checked `steps/` for phrase reuse first
  (DEFECT-04 lesson): none of the new step texts collide with existing ones; where behavior is
  genuinely identical (`requestId {string} has final status {string}`, `the processing summary
  is generated`, `the summary counts exactly ...`) the existing step definitions from
  `EndToEndSteps` are reused as-is rather than redefined.
- **Created** `transaction-service/src/test/resources/features/US-12_background_worker.feature`
  — `@US-12` feature with three scenarios:
  - `@AC-12.1` — a request submitted to the worker before `start()` is not touched by processing
    (status stays `RECEIVED`, not in the idempotency store); once the worker is started and
    drained it reaches `PROCESSED`. Proves requests are queued, not processed inline.
  - `@AC-12.2` — 3 valid requests submitted before `start()` are all still untouched; only after
    `start()` + `awaitDrained()` does `ProcessingSummary` correctly count all 3 as `PROCESSED`.
    Proves the ingesting thread isn't blocked per-request, and that the summary is only safe to
    trust once the worker signals drain.
  - `@AC-12.3` — reuses the existing US-05 fixture `txn2002_before_txn2001_section_9_8.json`
    (sequence 2 submitted before sequence 1, same account) routed through the worker instead of
    inline; both resolve to `PROCESSED` in the correct order, and `consumerThreadCount()` is
    asserted to be exactly 1 — proving the single global `SequenceBuffer` cursor is never raced
    by more than one consumer thread.

Manually sanity-checked the real `CommandLineRunner` via `mvn spring-boot:run`: processing now
visibly runs on a `[background-worker]`-named thread, the summary/balances print (on `[main]`)
only after that thread finishes, and the JVM still exits cleanly (daemon thread). Output is
byte-for-byte the same as before this change (`SUMMARY: PROCESSED=4 DUPLICATE=1 FAILED=1`,
`BALANCE ACC-201=85`, `ACC-202=0`, `ACC-203=500`), confirming no regression to the §9.8 batch
outcome.

## Pending
None — all three ACs (AC-12.1, AC-12.2, AC-12.3) have a passing, correctly-tagged scenario.

## Defects found this iteration
One design-ambiguity candidate, not a code defect (did not edit DESIGN.md):

- **LLD-01 vs DD-12 inconsistency on `BackgroundWorker`'s collaborator.** LLD-01's class diagram
  draws `BackgroundWorker --> Validator` as the only outgoing association, with no edge to
  `TransactionProcessor`. But DD-12's prose is explicit that the consumer thread "calls
  `TransactionProcessor.process(request)`" — and that's the only way the existing
  validate→dedupe→sequence→process→retry pipeline (all already orchestrated inside
  `TransactionProcessor.process`) stays intact without `BackgroundWorker` re-implementing or
  duplicating it. I followed DD-12's prose (the more detailed, authoritative decision text) and
  gave `BackgroundWorker` a `TransactionProcessor` field, not a `Validator` field — consistent
  with the diagram already being non-exhaustive elsewhere (e.g. `TransactionProcessor` itself
  lists no fields in LLD-01 even though it has five). Flagging for design-architect to reconcile
  the `BackgroundWorker --> Validator` arrow in LLD-01 with DD-12's prose, so the two don't drift
  further apart for the next person reading just the diagram.

## Fixes applied
None required — the inconsistency above was resolved by following DD-12's prose rather than by
changing any passing behavior; no test was failing because of it.

## Test evidence
`mvn test` from `transaction-service/`, full suite, after this change:

```
Tests run: 142, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Breakdown:
- Unit tests (JUnit): 103 passed (`InMemoryTransactionSourceTest` 6, `TransactionRequestTest` 14,
  `SequenceBufferTest` 8, `RetryPolicyTest` 4, `RequestSchemaValidatorTest` 18,
  `SchemaValidationResultTest` 2, `ProcessingSummaryTest` 3, `TransactionProcessorTest` 16,
  `ValidationResultTest` 2, `ValidatorTest` 10, `AccountLedgerTest` 12,
  `InMemoryIdempotencyStoreTest` 4, `AccountIdMaskingTest` 4).
- Cucumber (`RunCucumberTest`): 39 Scenarios (39 passed), 160 Steps (160 passed) — includes the
  3 new `US-12` scenarios (AC-12.1, AC-12.2, AC-12.3) and zero regressions across all previously
  passing US-01…US-11/end-to-end scenarios.
