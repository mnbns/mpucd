angular.module('assessmentApp')
.constant('DESIGN_DATA', {

  /* ---------------------------------------------------------------------
   * Requirement catalog — every FR / NFR from the problem statement gets
   * a stable ID + the anchor of the section it lives in on the
   * "Problem Statement" tab, so Design artifacts can link straight back.
   * ------------------------------------------------------------------- */
  requirements: [
    { id: 'L2-FR-01', category: 'FR', text: 'Accept transaction requests from a configurable or replaceable input source.', sectionAnchor: 'sec-9-4-functional-requirements' },
    { id: 'L2-FR-02', category: 'FR', text: 'Validate mandatory fields and reject malformed records.', sectionAnchor: 'sec-9-4-functional-requirements' },
    { id: 'L2-FR-03', category: 'FR', text: 'Maintain transaction processing state.', sectionAnchor: 'sec-9-4-functional-requirements' },
    { id: 'L2-FR-04', category: 'FR', text: 'Support a status lifecycle (RECEIVED, PROCESSING, PROCESSED, DUPLICATE, FAILED, RETRY_PENDING).', sectionAnchor: 'sec-9-4-functional-requirements' },
    { id: 'L2-FR-05', category: 'FR', text: 'Process each unique business transaction only once.', sectionAnchor: 'sec-9-4-functional-requirements' },
    { id: 'L2-FR-06', category: 'FR', text: 'Detect duplicate requests even if they arrive later in the batch.', sectionAnchor: 'sec-9-4-functional-requirements' },
    { id: 'L2-FR-07', category: 'FR', text: 'Define and implement a strategy for out-of-order events.', sectionAnchor: 'sec-9-4-functional-requirements' },
    { id: 'L2-FR-08', category: 'FR', text: 'Simulate at least one retryable failure scenario.', sectionAnchor: 'sec-9-4-functional-requirements' },
    { id: 'L2-FR-09', category: 'FR', text: 'Retry failed transient records without creating duplicate effects.', sectionAnchor: 'sec-9-4-functional-requirements' },
    { id: 'L2-FR-10', category: 'FR', text: 'Separate business processing logic from input handling where practical.', sectionAnchor: 'sec-9-4-functional-requirements' },
    { id: 'L2-FR-11', category: 'FR', text: 'Produce a processing summary with counts by status.', sectionAnchor: 'sec-9-4-functional-requirements' },
    { id: 'L2-FR-12', category: 'FR', text: 'Include tests for duplicate, out-of-order, invalid, and retry scenarios.', sectionAnchor: 'sec-9-4-functional-requirements' },

    { id: 'NFR-01', category: 'NFR', text: 'Idempotency — duplicate requests must not cause duplicate effects.', sectionAnchor: 'sec-6-1-idempotency' },
    { id: 'NFR-02', category: 'NFR', text: 'Ordering — tolerate requests arriving in any order.', sectionAnchor: 'sec-6-2-ordering' },
    { id: 'NFR-03', category: 'NFR', text: 'Resilience — handle transient failures with safe retries.', sectionAnchor: 'sec-6-3-resilience' },
    { id: 'NFR-04', category: 'NFR', text: 'Data Consistency — no partially updated transaction state.', sectionAnchor: 'sec-6-4-data-consistency' },
    { id: 'NFR-05', category: 'NFR', text: 'Observability — logs + operational visibility.', sectionAnchor: 'sec-6-5-observability' },
    { id: 'NFR-06', category: 'NFR', text: 'Security & Data Handling — validation, no secrets, no sensitive logs.', sectionAnchor: 'sec-6-6-security-and-data-handling' },
    { id: 'NFR-07', category: 'NFR', text: 'Maintainability — modular, testable, separated concerns.', sectionAnchor: 'sec-6-7-maintainability' }
  ],

  /* ---------------------------------------------------------------------
   * User Stories + Acceptance Criteria.
   * Every US carries: its own id, the requirement id(s) it traces to,
   * and the diagram ids that model it — all rendered as clickable badges.
   * ------------------------------------------------------------------- */
  userStories: [
    {
      id: 'US-01',
      title: 'Configurable Transaction Ingestion',
      priority: 'Must',
      asA: 'operator',
      iWant: 'the service to accept transaction requests from a configurable, replaceable input source',
      soThat: 'the same core logic can later be wired to a real queue, file, or API without touching business logic',
      linkedRequirements: ['L2-FR-01'],
      linkedDiagrams: ['HLD-01', 'LLD-01'],
      acceptanceCriteria: [
        { id: 'AC-01.1', given: 'an in-memory list of transaction requests', when: 'the service starts', then: 'it loads all requests through a single TransactionSource interface.' },
        { id: 'AC-01.2', given: 'the input source implementation is swapped (e.g. for a file reader)', when: 'the service runs', then: 'no change is required in the validator, processor, or state-store classes.' }
      ]
    },
    {
      id: 'US-02',
      title: 'Input Validation',
      priority: 'Must',
      asA: 'system owner',
      iWant: 'malformed transaction records rejected before processing',
      soThat: 'bad data cannot corrupt transaction state or crash the batch',
      linkedRequirements: ['L2-FR-02', 'NFR-06'],
      linkedDiagrams: ['HLD-01', 'FC-01', 'LLD-01'],
      acceptanceCriteria: [
        { id: 'AC-02.1', given: 'a request missing a mandatory field (transactionId, requestId, accountId, amount, type)', when: 'it is ingested', then: 'it is marked FAILED with a validation reason and excluded from business processing.' },
        { id: 'AC-02.2', given: 'a request with a negative amount that violates the field rules (e.g. TXN-2004, amount = -10)', when: 'it is validated', then: 'it is rejected as FAILED with a clear reason, without halting the rest of the batch.' },
        { id: 'AC-02.3', given: 'one invalid record inside a batch of N records', when: 'the batch runs', then: 'the remaining N-1 valid records are still processed normally.' }
      ]
    },
    {
      id: 'US-03',
      title: 'Transaction State Lifecycle',
      priority: 'Must',
      asA: 'QA / operations reviewer',
      iWant: 'every transaction to carry a well-defined, enforced status',
      soThat: 'I can always tell what happened to a given request without reading code',
      linkedRequirements: ['L2-FR-03', 'L2-FR-04'],
      linkedDiagrams: ['LLD-01', 'FC-01'],
      acceptanceCriteria: [
        { id: 'AC-03.1', given: 'a new request', when: 'it is ingested', then: 'its initial status is RECEIVED.' },
        { id: 'AC-03.2', given: 'a RECEIVED request picked up for work', when: 'processing starts', then: 'status transitions to PROCESSING.' },
        { id: 'AC-03.3', given: 'any transaction at any point in time', when: 'its status is inspected', then: 'it is exactly one of RECEIVED, PROCESSING, PENDING_ORDER, RETRY_PENDING, PROCESSED, DUPLICATE, FAILED.' },
        { id: 'AC-03.4', given: 'an illegal transition is attempted (e.g. DUPLICATE → PROCESSING)', when: 'it is attempted', then: 'it is rejected and logged as an invalid transition.' }
      ]
    },
    {
      id: 'US-04',
      title: 'Idempotent Processing & Duplicate Detection',
      priority: 'Must',
      asA: 'business owner',
      iWant: 'each unique transaction processed exactly once regardless of retries or replays',
      soThat: 'customers are never double-charged or double-credited',
      linkedRequirements: ['L2-FR-05', 'L2-FR-06', 'NFR-01'],
      linkedDiagrams: ['HLD-01', 'FC-02', 'SEQ-01', 'LLD-01'],
      acceptanceCriteria: [
        { id: 'AC-04.1', given: 'a requestId already marked PROCESSED', when: 'the same requestId is submitted again', then: 'it is marked DUPLICATE and no balance/ledger side effect occurs.' },
        { id: 'AC-04.2', given: 'REQ-2 and REQ-2-DUP both referencing TXN-2002 (§9.8, rows 1 & 3)', when: 'REQ-2 is processed first', then: 'REQ-2-DUP is detected as DUPLICATE even though it arrives later in the batch.' },
        { id: 'AC-04.3', given: 'the idempotency store keyed by requestId', when: 'the same request is submitted N times', then: 'the final ledger effect is identical to processing it exactly once.' }
      ]
    },
    {
      id: 'US-05',
      title: 'Out-of-Order Request Handling (Option A — Sequence-Based)',
      priority: 'Must',
      asA: 'system',
      iWant: 'to process a request only after all of its prior required sequence numbers have arrived',
      soThat: 'account balances are always computed on a consistent, ordered history',
      linkedRequirements: ['L2-FR-07', 'NFR-02'],
      linkedDiagrams: ['HLD-01', 'FC-03', 'SEQ-01', 'LLD-01'],
      acceptanceCriteria: [
        { id: 'AC-05.1', given: 'TXN-2002 (seq=2) arrives before TXN-2001 (seq=1) for the same account (§9.8, rows 1 & 2)', when: 'seq=2 is received', then: 'it is held as PENDING_ORDER, not processed.' },
        { id: 'AC-05.2', given: 'seq=1 later arrives and is processed', when: 'processing completes', then: 'the buffered seq=2 request is automatically released and processed next, in order.' },
        { id: 'AC-05.3', given: 'a sequence number that never arrives within the batch', when: 'end-of-batch is reached', then: 'the missing sequence is reported in the processing summary, not silently dropped.' }
      ]
    },
    {
      id: 'US-06',
      title: 'Retry-Safe Resilience',
      priority: 'Must',
      asA: 'operator',
      iWant: 'transient failures retried automatically without duplicating effects',
      soThat: 'temporary downstream issues never cause lost or duplicated transactions',
      linkedRequirements: ['L2-FR-08', 'L2-FR-09', 'NFR-03'],
      linkedDiagrams: ['HLD-01', 'FC-04', 'SEQ-01', 'LLD-01'],
      acceptanceCriteria: [
        { id: 'AC-06.1', given: 'a transaction simulated to fail transiently (e.g. TXN-2005, §9.8 row 6)', when: 'processing fails', then: 'status becomes RETRY_PENDING and retryCount is incremented.' },
        { id: 'AC-06.2', given: 'a transaction retried up to the configured max retry count and still failing', when: 'the last retry fails', then: 'status becomes FAILED with a recorded reason, and no partial effect remains.' },
        { id: 'AC-06.3', given: 'a transaction that succeeds on retry attempt K', when: 'it succeeds', then: 'it is marked PROCESSED exactly once — the idempotency store prevents any earlier partial attempt from also taking effect.' }
      ]
    },
    {
      id: 'US-07',
      title: 'Data Consistency',
      priority: 'Should',
      asA: 'system owner',
      iWant: 'transaction processing to be atomic from the caller’s point of view',
      soThat: 'no transaction is ever left half-applied',
      linkedRequirements: ['NFR-04'],
      linkedDiagrams: ['FC-01', 'SEQ-01'],
      acceptanceCriteria: [
        { id: 'AC-07.1', given: 'a transaction being processed', when: 'a failure occurs mid-step', then: 'the visible state is either fully PROCESSED or fully FAILED/RETRY_PENDING — never a mix of "balance updated but status not updated".' },
        { id: 'AC-07.2', given: 'concurrent requests for the same accountId', when: 'both are processed', then: 'updates are serialized per account to avoid lost updates.' }
      ]
    },
    {
      id: 'US-08',
      title: 'Observability & Processing Summary',
      priority: 'Must',
      asA: 'SDET / operations reviewer',
      iWant: 'structured logs and a summary report at the end of a run',
      soThat: 'I can verify outcomes without manually inspecting code',
      linkedRequirements: ['L2-FR-11', 'NFR-05'],
      linkedDiagrams: ['HLD-01', 'FC-01'],
      acceptanceCriteria: [
        { id: 'AC-08.1', given: 'a batch run has completed', when: 'the summary is generated', then: 'it reports counts by status (RECEIVED, PROCESSED, DUPLICATE, FAILED, RETRY_PENDING, PENDING_ORDER).' },
        { id: 'AC-08.2', given: 'any transaction event (received, duplicate, retried, failed)', when: 'it occurs', then: 'a structured log line is emitted with transactionId, requestId, status, and timestamp — no sensitive fields.' }
      ]
    },
    {
      id: 'US-09',
      title: 'Security & Safe Data Handling',
      priority: 'Should',
      asA: 'security-conscious engineer',
      iWant: 'sensitive fields protected and all input validated',
      soThat: 'the service never leaks data or accepts unsafe input',
      linkedRequirements: ['NFR-06'],
      linkedDiagrams: ['LLD-01'],
      acceptanceCriteria: [
        { id: 'AC-09.1', given: 'any log statement for a transaction', when: 'it is emitted', then: 'it never includes full account numbers or PII beyond a masked/short accountId reference.' },
        { id: 'AC-09.2', given: 'the service configuration', when: 'it is reviewed', then: 'no secrets or credentials are hardcoded in source — all are externalized.' }
      ]
    },
    {
      id: 'US-10',
      title: 'Maintainable, Modular Design',
      priority: 'Should',
      asA: 'future maintainer',
      iWant: 'business rules separated from input/infrastructure code',
      soThat: 'the service can evolve (e.g. swap in-memory store for a real DB) with minimal risk',
      linkedRequirements: ['L2-FR-10', 'NFR-07'],
      linkedDiagrams: ['LLD-01'],
      acceptanceCriteria: [
        { id: 'AC-10.1', given: 'the TransactionProcessor (business logic)', when: 'it is inspected', then: 'it has no direct dependency on how input is sourced — only on a TransactionSource interface.' },
        { id: 'AC-10.2', given: 'the idempotency store', when: 'it is inspected', then: 'it sits behind an IdempotencyStore interface so an in-memory map can later be swapped for Redis/DB without touching business logic.' }
      ]
    },
    {
      id: 'US-11',
      title: 'Automated Test Coverage (Cucumber BDD, one feature file per User Story)',
      priority: 'Must',
      asA: 'quality owner',
      iWant: 'automated, readable tests for every critical scenario, organized one Cucumber feature file per User Story with one Scenario per Acceptance Criterion',
      soThat: 'regressions in idempotency, ordering, or retry logic are caught immediately, and the spec and the test are always in lock-step',
      linkedRequirements: ['L2-FR-12'],
      linkedDiagrams: ['FC-01', 'FC-02', 'FC-03', 'FC-04'],
      acceptanceCriteria: [
        { id: 'AC-11.1', given: 'the §9.8 suggested test data set replayed in order by end_to_end_batch.feature', when: 'the full Cucumber suite runs', then: 'there is at least one passing scenario per case: duplicate, out-of-order, invalid/validation failure, and retry-to-success / retry-exhaustion.' },
        { id: 'AC-11.2', given: 'a regression introduced in the idempotency check', when: 'tests run', then: 'at least one test fails — idempotency has direct, not incidental, coverage.' }
      ]
    }
  ],

  /* ---------------------------------------------------------------------
   * HLD / LLD / Flowcharts / Sequence — each a Mermaid source string.
   * ------------------------------------------------------------------- */
  diagrams: {
    hld: {
      id: 'HLD-01',
      title: 'High-Level Architecture',
      description: 'Component-level view of the pipeline: ingestion → validation → idempotency check → ordering → processing → retry → state → observability.',
      mermaid:
'flowchart LR\n' +
'  classDef comp fill:#eef3ff,stroke:#3b5bdb,color:#1a2746;\n' +
'  classDef store fill:#fff7e6,stroke:#d9822b,color:#4a2f00;\n' +
'  classDef terminal fill:#eafaf1,stroke:#2f9e44,color:#0b3d1f;\n' +
'  classDef bad fill:#fff0f0,stroke:#c92a2a,color:#4a0a0a;\n' +
'\n' +
'  SRC["TransactionSource<br/>(list / file / queue adapter)<br/>L2-FR-01"]:::comp\n' +
'  VAL["Validator<br/>L2-FR-02"]:::comp\n' +
'  IDEMP["Idempotency Store<br/>NFR-01"]:::store\n' +
'  SEQBUF["Sequence Buffer<br/>NFR-02 (Option A)"]:::comp\n' +
'  PROC["Transaction Processor<br/>L2-FR-05"]:::comp\n' +
'  STATEDB["Transaction State Store<br/>L2-FR-03"]:::store\n' +
'  RETRY["Retry Handler<br/>L2-FR-08 / L2-FR-09"]:::comp\n' +
'  LOGS["Structured Logger"]:::comp\n' +
'  SUMMARY["Processing Summary<br/>L2-FR-11"]:::comp\n' +
'\n' +
'  F_FAILED["FAILED"]:::bad\n' +
'  F_DUP["DUPLICATE"]:::terminal\n' +
'  F_PENDING["PENDING_ORDER"]:::terminal\n' +
'  F_PROCESSED["PROCESSED"]:::terminal\n' +
'  F_FAILED2["FAILED (retries exhausted)"]:::bad\n' +
'\n' +
'  SRC --> VAL\n' +
'  VAL -- valid --> IDEMP\n' +
'  VAL -- invalid --> F_FAILED\n' +
'  IDEMP -- duplicate --> F_DUP\n' +
'  IDEMP -- new --> SEQBUF\n' +
'  SEQBUF -- "in order" --> PROC\n' +
'  SEQBUF -- "future seq" --> F_PENDING\n' +
'  PROC -- success --> STATEDB\n' +
'  STATEDB --> F_PROCESSED\n' +
'  PROC -- "transient failure" --> RETRY\n' +
'  RETRY -- "retry ok" --> PROC\n' +
'  RETRY -- exhausted --> F_FAILED2\n' +
'  F_FAILED --> LOGS\n' +
'  F_DUP --> LOGS\n' +
'  F_PENDING --> LOGS\n' +
'  F_PROCESSED --> LOGS\n' +
'  F_FAILED2 --> LOGS\n' +
'  LOGS --> SUMMARY\n',
      linkedRequirements: ['L2-FR-01', 'L2-FR-02', 'L2-FR-03', 'L2-FR-05', 'L2-FR-08', 'L2-FR-09', 'L2-FR-11', 'NFR-01', 'NFR-02']
    },

    lld: {
      id: 'LLD-01',
      title: 'Domain & Component Class Diagram',
      description: 'Classes, their fields/methods, and the dependency wiring that keeps business logic separate from infrastructure.',
      mermaid:
'classDiagram\n' +
'  class TransactionRequest {\n' +
'    -String transactionId\n' +
'    -String requestId\n' +
'    -String accountId\n' +
'    -TransactionType transactionType\n' +
'    -BigDecimal amount\n' +
'    -String currency\n' +
'    -long sequenceNumber\n' +
'    -TransactionStatus status\n' +
'    -int retryCount\n' +
'    -String failureReason\n' +
'  }\n' +
'  class TransactionStatus {\n' +
'    <<enumeration>>\n' +
'    RECEIVED\n' +
'    PROCESSING\n' +
'    PENDING_ORDER\n' +
'    RETRY_PENDING\n' +
'    PROCESSED\n' +
'    DUPLICATE\n' +
'    FAILED\n' +
'  }\n' +
'  class TransactionType {\n' +
'    <<enumeration>>\n' +
'    CREDIT\n' +
'    DEBIT\n' +
'    REVERSAL\n' +
'  }\n' +
'  class TransactionSource {\n' +
'    <<interface>>\n' +
'    +List~TransactionRequest~ fetchBatch()\n' +
'  }\n' +
'  class Validator {\n' +
'    +ValidationResult validate(TransactionRequest)\n' +
'  }\n' +
'  class IdempotencyStore {\n' +
'    <<interface>>\n' +
'    +boolean isProcessed(String requestId)\n' +
'    +void markProcessed(String requestId)\n' +
'  }\n' +
'  class SequenceBuffer {\n' +
'    -long lastResolvedSequence\n' +
'    +boolean isReadyToProcess(TransactionRequest)\n' +
'    +boolean isAlreadyResolved(TransactionRequest)\n' +
'    +void buffer(TransactionRequest)\n' +
'    +void markResolved(TransactionRequest)\n' +
'    +TransactionRequest pollReady()\n' +
'  }\n' +
'  class TransactionProcessor {\n' +
'    +void process(TransactionRequest)\n' +
'  }\n' +
'  class RetryPolicy {\n' +
'    -int maxRetries\n' +
'    +boolean shouldRetry(TransactionRequest)\n' +
'  }\n' +
'  class ProcessingSummary {\n' +
'    -Map~TransactionStatus,Integer~ countsByStatus\n' +
'    +void record(TransactionStatus)\n' +
'    +String report()\n' +
'  }\n' +
'\n' +
'  TransactionSource ..> TransactionRequest\n' +
'  Validator ..> TransactionRequest\n' +
'  TransactionProcessor --> IdempotencyStore\n' +
'  TransactionProcessor --> SequenceBuffer\n' +
'  TransactionProcessor --> RetryPolicy\n' +
'  TransactionProcessor --> ProcessingSummary\n' +
'  TransactionProcessor ..> TransactionRequest\n' +
'  TransactionRequest --> TransactionStatus\n' +
'  TransactionRequest --> TransactionType\n',
      linkedRequirements: ['L2-FR-01', 'L2-FR-03', 'L2-FR-04', 'L2-FR-10', 'NFR-01', 'NFR-02', 'NFR-07']
    },

    flows: [
      {
        id: 'FC-01',
        title: 'End-to-End Decision Flow',
        description: 'One request’s full journey through every decision point, from receipt to terminal status.',
        mermaid:
'flowchart TD\n' +
'  A["Request received"] --> B{"Mandatory fields valid?"}\n' +
'  B -- No --> Z1["FAILED: validation error"]\n' +
'  B -- Yes --> C{"requestId already processed?"}\n' +
'  C -- Yes --> Z2["DUPLICATE"]\n' +
'  C -- No --> D{"sequenceNumber is next expected for accountId?"}\n' +
'  D -- "No, future" --> Z3["PENDING_ORDER (buffered)"]\n' +
'  D -- Yes --> E["PROCESSING: apply business rule"]\n' +
'  E --> F{"Processing succeeded?"}\n' +
'  F -- Yes --> G["PROCESSED: record in Idempotency Store"]\n' +
'  F -- "No, transient" --> H{"retryCount < maxRetries?"}\n' +
'  H -- Yes --> I["RETRY_PENDING: retryCount++"]\n' +
'  I --> E\n' +
'  H -- No --> Z4["FAILED: retries exhausted"]\n' +
'  G --> J["Release next buffered sequence, if any"]\n' +
'  Z1 --> K["Log event + update Processing Summary"]\n' +
'  Z2 --> K\n' +
'  Z3 --> K\n' +
'  Z4 --> K\n' +
'  G --> K\n',
        linkedRequirements: ['L2-FR-02', 'L2-FR-04', 'L2-FR-05', 'L2-FR-07', 'L2-FR-08', 'L2-FR-09', 'L2-FR-11', 'NFR-04']
      },
      {
        id: 'FC-02',
        title: 'Idempotency / Duplicate-Detection Flow',
        description: 'Zoom-in on the idempotency check referenced by US-04.',
        mermaid:
'flowchart TD\n' +
'  A["Incoming request with requestId"] --> B{"requestId key exists in Idempotency Store?"}\n' +
'  B -- Yes --> C["Mark status = DUPLICATE"]\n' +
'  C --> D["No side effect applied"]\n' +
'  D --> E["Log event + increment DUPLICATE count"]\n' +
'  B -- No --> F["Proceed to ordering check"]\n' +
'  F --> G["On success: write requestId key to store"]\n',
        linkedRequirements: ['L2-FR-05', 'L2-FR-06', 'NFR-01']
      },
      {
        id: 'FC-03',
        title: 'Out-of-Order / Sequence Buffer Flow',
        description: 'Zoom-in on Option A (sequence-based buffering) referenced by US-05. One cursor for the whole batch, not per account (DD-02) - verified against §9.8.',
        mermaid:
'flowchart TD\n' +
'  A["Valid, non-duplicate request<br/>(has a sequenceNumber)"] --> B{"sequenceNumber == lastResolvedSequence + 1?"}\n' +
'  B -- Yes --> C["Process immediately"]\n' +
'  C --> D["lastResolvedSequence = sequenceNumber<br/>(on success OR definitive failure)"]\n' +
'  D --> E{"Buffered request exists for next sequence?"}\n' +
'  E -- Yes --> C\n' +
'  E -- No --> F["Done for now"]\n' +
'  B -- "No, seq is in the future" --> G["Mark PENDING_ORDER"]\n' +
'  G --> H["Buffer request keyed by sequenceNumber"]\n' +
'  B -- "No, seq <= lastResolvedSequence" --> I["Treat as stale / DUPLICATE"]\n',
        linkedRequirements: ['L2-FR-07', 'NFR-02']
      },
      {
        id: 'FC-04',
        title: 'Retry / Failure Flow',
        description: 'Zoom-in on retry-safe resilience referenced by US-06.',
        mermaid:
'flowchart TD\n' +
'  A["Attempt to process transaction"] --> B{"Outcome?"}\n' +
'  B -- Success --> C["Status = PROCESSED"]\n' +
'  C --> D["Write requestId to Idempotency Store"]\n' +
'  B -- "Transient failure" --> E{"retryCount < maxRetries?"}\n' +
'  E -- Yes --> F["retryCount = retryCount + 1"]\n' +
'  F --> G["Status = RETRY_PENDING"]\n' +
'  G --> A\n' +
'  E -- No --> H["Status = FAILED"]\n' +
'  H --> I["Record failureReason: retries exhausted"]\n' +
'  B -- "Permanent / validation failure" --> H\n',
        linkedRequirements: ['L2-FR-08', 'L2-FR-09', 'NFR-03']
      }
    ],

    sequence: {
      id: 'SEQ-01',
      title: 'End-to-End Request Sequence',
      description: 'Cross-component interaction for one request from ingestion to summary update.',
      mermaid:
'sequenceDiagram\n' +
'  participant S as TransactionSource\n' +
'  participant V as Validator\n' +
'  participant I as IdempotencyStore\n' +
'  participant Q as SequenceBuffer\n' +
'  participant P as TransactionProcessor\n' +
'  participant R as RetryPolicy\n' +
'  participant L as ObservabilityLogger\n' +
'\n' +
'  S->>V: fetchBatch() returns TransactionRequest\n' +
'  V->>V: validate()\n' +
'  alt invalid\n' +
'    V->>L: log FAILED (validation)\n' +
'  else valid\n' +
'    V->>I: isProcessed(requestId)?\n' +
'    alt already processed\n' +
'      I->>L: log DUPLICATE\n' +
'    else new request\n' +
'      V->>Q: isReadyToProcess(sequenceNumber)?\n' +
'      alt future sequence\n' +
'        Q->>L: log PENDING_ORDER (buffered)\n' +
'      else ready to process\n' +
'        Q->>P: process(request)\n' +
'        P->>P: apply business rule\n' +
'        alt success\n' +
'          P->>I: markProcessed(requestId)\n' +
'          P->>L: log PROCESSED\n' +
'        else transient failure\n' +
'          P->>R: shouldRetry(retryCount)?\n' +
'          alt retry allowed\n' +
'            R->>P: schedule retry\n' +
'            P->>L: log RETRY_PENDING\n' +
'          else retries exhausted\n' +
'            P->>L: log FAILED\n' +
'          end\n' +
'        end\n' +
'        Q->>Q: release next buffered sequence if ready\n' +
'      end\n' +
'    end\n' +
'  end\n' +
'  L->>L: update ProcessingSummary counts by status\n',
      linkedRequirements: ['L2-FR-05', 'L2-FR-06', 'L2-FR-07', 'L2-FR-08', 'L2-FR-09', 'L2-FR-11', 'NFR-01', 'NFR-02', 'NFR-03']
    }
  },

  /* ---------------------------------------------------------------------
   * Design decisions worth calling out explicitly in review.
   * ------------------------------------------------------------------- */
  decisions: [
    {
      id: 'DD-01',
      title: 'Stack: Java 17 + Spring Boot, CommandLineRunner (no web layer, no DB)',
      rationale: 'Ground rules (§7) explicitly allow Java; no production DB or complex UI is mandated. A console-style batch run keeps the 2-hour build boring and testable.',
      linkedRequirements: ['L2-FR-01', 'NFR-07']
    },
    {
      id: 'DD-02',
      title: 'Out-of-order strategy: Option A, sequence-based buffering - one cursor for the whole batch, not per account',
      rationale: 'The §9.8 test data is ordered by sequence (TXN-2002 seq=2 arrives before TXN-2001 seq=1), so Option A fits directly. The cursor is batch-wide, not per accountId as first drafted: with a per-account cursor, ACC-203’s one and only transaction (sequence 5, no sequence 1-4 ever exists for that account) could never become "ready" and would never reach the retry logic - contradicting §9.8’s own expected outcome for that row. Running the engine against the real §9.8 data surfaced this; a single cursor, advanced on every resolved sequence number including validation failures (so one permanently failed record never blocks everything after it), reproduces all six §9.8 expected outcomes exactly. Option C (eventTime/version, "latest wins") remains the noted production-scale alternative for a real event-sourced ledger.',
      linkedRequirements: ['L2-FR-07', 'NFR-02']
    },
    {
      id: 'DD-03',
      title: 'Idempotency key = requestId (not transactionId)',
      rationale: 'transactionId identifies the business transaction; requestId/idempotencyKey identifies the physical attempt. Keying the dedup store on requestId lets us detect exact-duplicate retries (REQ-2 vs REQ-2-DUP) while still reasoning about transactionId for ordering/sequencing.',
      linkedRequirements: ['L2-FR-05', 'L2-FR-06', 'NFR-01']
    },
    {
      id: 'DD-04',
      title: 'Max retry count = 3, in-memory exponential-ish backoff simulation',
      rationale: 'Simple, bounded, and easy to assert in unit tests; a real deployment would externalize this as config and use a scheduler/queue redelivery count instead of an in-process loop.',
      linkedRequirements: ['L2-FR-08', 'L2-FR-09', 'NFR-03']
    },
    {
      id: 'DD-05',
      title: 'Production hardening notes (explained, not built)',
      rationale: 'Swap in-memory IdempotencyStore/StateStore for Redis/DB with a unique constraint on requestId; swap TransactionSource for a real queue (Kafka/SQS) with consumer-group ordering per accountId as partition key; add per-account locking or optimistic concurrency for NFR-04; add metrics/tracing (Micrometer) for NFR-05 beyond console logs.',
      linkedRequirements: ['NFR-01', 'NFR-02', 'NFR-04', 'NFR-05']
    },
    {
      id: 'DD-06',
      title: 'Test strategy: Cucumber BDD, one feature file per User Story, step definitions call the engine directly (no HTTP)',
      rationale: 'L2-FR-12 asks for tests covering duplicate, out-of-order, invalid, and retry scenarios without mandating a shape. Structuring one .feature file per User Story, with one Scenario per Acceptance Criterion (same Given/When/Then text as the AC table above), keeps the spec and the test in lock-step and gives a non-engineer reviewer something readable. Step definitions call TransactionProcessor and friends as plain Java objects — consistent with DD-01 (console/in-memory, no web layer). A final end_to_end_batch.feature replays the whole §9.8 set as one ordered, stateful scenario, which is what actually demonstrates US-11 (coverage is shown by the suite existing and passing, not by a scenario about scenarios).',
      linkedRequirements: ['L2-FR-12']
    }
  ],

  /* ---------------------------------------------------------------------
   * BDD plan: one Cucumber .feature file per User Story (Scenario = AC),
   * plus the end-to-end batch feature. Lives under
   * transaction-service/src/test/resources/features/.
   * ------------------------------------------------------------------- */
  bddPlan: [
    { usId: 'US-01', file: 'US-01_configurable_ingestion.feature' },
    { usId: 'US-02', file: 'US-02_input_validation.feature' },
    { usId: 'US-03', file: 'US-03_transaction_state_lifecycle.feature' },
    { usId: 'US-04', file: 'US-04_idempotent_processing.feature' },
    { usId: 'US-05', file: 'US-05_out_of_order_handling.feature' },
    { usId: 'US-06', file: 'US-06_retry_resilience.feature' },
    { usId: 'US-07', file: 'US-07_data_consistency.feature' },
    { usId: 'US-08', file: 'US-08_observability_summary.feature' },
    { usId: 'US-09', file: 'US-09_security_data_handling.feature' },
    { usId: 'US-10', file: 'US-10_maintainable_design.feature' },
    { usId: 'US-11', file: 'end_to_end_batch.feature', note: 'Single stateful scenario replaying the full §9.8 batch in order — the concrete expression of "automated test coverage", not a per-AC mapping.' }
  ]
});
