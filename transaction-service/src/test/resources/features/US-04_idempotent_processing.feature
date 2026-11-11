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
    Given two requests referencing the same business transaction, as in section 9.8 rows 1 and 3, loaded from "testdata/US-04_idempotent_processing/req2_dup_section_9_8.json"
    When the first request is processed first
    Then the duplicate request is detected as DUPLICATE even though it arrives later in the batch

  @AC-04.3
  Scenario: Repeated submissions yield exactly one effect
    Given the idempotency store keyed by requestId
    When the same request is submitted multiple times
    Then the final ledger effect is identical to processing it exactly once

  @AC-04.4
  Scenario: A stale sequence number is treated as a duplicate even with brand-new identifiers
    Given a batch where sequence 1 through 3 have already resolved, loaded from "testdata/US-04_idempotent_processing/stale_sequence_replay.json"
    When a brand-new request reuses an already-resolved sequence number
    Then it is marked DUPLICATE
    And no balance or ledger side effect occurs
