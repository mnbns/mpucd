@US-11
Feature: End-to-End Transaction Batch Processing
  Traces to: L2-FR-12
  This feature is the concrete expression of US-11 (Automated Test Coverage):
  it replays the full section 9.8 sample batch as one ordered, stateful run and
  checks that duplicate detection, out-of-order buffering, validation, and retry
  all cooperate correctly together - not just in isolation.

  @AC-11.1
  Scenario: The full section 9.8 sample batch processes to the documented outcomes
    Given the full section 9.8 sample batch is submitted in its documented input order
    When the batch finishes processing, including any retries
    # REQ-2 is released once REQ-1 fills sequence 1; REQ-4 fails validation (negative amount);
    # REQ-5 resolves to PROCESSED after one simulated transient failure and one retry.
    Then requestId "REQ-1" has final status "PROCESSED"
    And requestId "REQ-2" has final status "PROCESSED"
    And requestId "REQ-2-DUP" has final status "DUPLICATE"
    And requestId "REQ-3" has final status "PROCESSED"
    And requestId "REQ-4" has final status "FAILED"
    And requestId "REQ-5" has final status "PROCESSED"

  @AC-11.1
  Scenario: The processing summary reconciles with the batch outcome
    Given the full section 9.8 sample batch is submitted in its documented input order
    When the batch finishes processing, including any retries
    And the processing summary is generated
    Then the summary counts exactly 4 PROCESSED, 1 DUPLICATE, and 1 FAILED
    And no count is missing or double-counted
