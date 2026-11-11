@US-02
Feature: Input Validation
  Traces to: L2-FR-02, NFR-06

  As a system owner
  I want malformed transaction records rejected before processing
  So that bad data cannot corrupt transaction state or crash the batch

  @AC-02.1
  Scenario: Missing mandatory field is rejected
    Given a request missing a mandatory field such as transactionId, requestId, accountId, amount, or type
    When it is ingested
    Then it is marked FAILED with a validation reason
    And it is excluded from business processing

  @AC-02.2
  Scenario: Invalid amount is rejected with a clear reason
    Given a request with a negative amount that violates the field rules, such as TXN-2004 with amount -10
    When it is validated
    Then it is rejected as FAILED with a clear reason
    And the rest of the batch still processes

  @AC-02.3
  Scenario: One invalid record does not stop the batch
    Given one invalid record inside a batch of N records
    When the batch runs
    Then the remaining N-1 valid records are still processed normally
