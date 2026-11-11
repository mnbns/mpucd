@US-11
Feature: End-to-End Transaction Batch Processing
  Traces to: L2-FR-12
  This feature is the concrete expression of US-11 (Automated Test Coverage):
  it replays the full section 9.8 sample batch as one ordered, stateful run and
  checks that duplicate detection, out-of-order buffering, validation, and retry
  all cooperate correctly together - not just in isolation.

  @AC-11.1
  Scenario: The full section 9.8 sample batch processes to the documented outcomes
    Given the following transaction requests are submitted in this exact order:
      | inputOrder | transactionId | requestId  | sequence | accountId | type   | amount |
      | 1          | TXN-2002      | REQ-2      | 2        | ACC-201   | DEBIT  | 40     |
      | 2          | TXN-2001      | REQ-1      | 1        | ACC-201   | CREDIT | 100    |
      | 3          | TXN-2002      | REQ-2-DUP  | 2        | ACC-201   | DEBIT  | 40     |
      | 4          | TXN-2003      | REQ-3      | 3        | ACC-201   | CREDIT | 25     |
      | 5          | TXN-2004      | REQ-4      | 4        | ACC-202   | DEBIT  | -10    |
      | 6          | TXN-2005      | REQ-5      | 5        | ACC-203   | CREDIT | 500    |
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
    Given the following transaction requests are submitted in this exact order:
      | inputOrder | transactionId | requestId  | sequence | accountId | type   | amount |
      | 1          | TXN-2002      | REQ-2      | 2        | ACC-201   | DEBIT  | 40     |
      | 2          | TXN-2001      | REQ-1      | 1        | ACC-201   | CREDIT | 100    |
      | 3          | TXN-2002      | REQ-2-DUP  | 2        | ACC-201   | DEBIT  | 40     |
      | 4          | TXN-2003      | REQ-3      | 3        | ACC-201   | CREDIT | 25     |
      | 5          | TXN-2004      | REQ-4      | 4        | ACC-202   | DEBIT  | -10    |
      | 6          | TXN-2005      | REQ-5      | 5        | ACC-203   | CREDIT | 500    |
    When the batch finishes processing, including any retries
    And the processing summary is generated
    Then the summary counts exactly 4 PROCESSED, 1 DUPLICATE, and 1 FAILED
    And no count is missing or double-counted
