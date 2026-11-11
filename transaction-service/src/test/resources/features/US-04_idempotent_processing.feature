@US-04
Feature: Idempotent Processing and Duplicate Detection
  Traces to: L2-FR-05, L2-FR-06, NFR-01

  As a business owner
  I want each unique transaction processed exactly once regardless of retries or replays
  So that customers are never double-charged or double-credited

  @AC-04.1
  Scenario: Already-processed requestId is marked DUPLICATE
    Given a requestId already marked PROCESSED
    When the same requestId is submitted again
    Then it is marked DUPLICATE
    And no balance or ledger side effect occurs

  @AC-04.2
  Scenario: A duplicate arriving later in the batch is still detected
    Given REQ-2 and REQ-2-DUP both referencing TXN-2002, as in section 9.8 rows 1 and 3
    When REQ-2 is processed first
    Then REQ-2-DUP is detected as DUPLICATE even though it arrives later in the batch

  @AC-04.3
  Scenario: Repeated submissions yield exactly one effect
    Given the idempotency store keyed by requestId
    When the same request is submitted multiple times
    Then the final ledger effect is identical to processing it exactly once
