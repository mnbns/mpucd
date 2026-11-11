@US-05
Feature: Out-of-Order Request Handling (Option A - Sequence-Based)
  Traces to: L2-FR-07, NFR-02

  As the system
  I want to process a request only after all of its prior required sequence numbers have arrived
  So that account balances are always computed on a consistent, ordered history

  @AC-05.1 @AC-05.2
  Scenario: A future sequence is buffered, then released once its predecessor arrives
    Given TXN-2002 with sequence 2 arrives before TXN-2001 with sequence 1 for the same account, as in section 9.8 rows 1 and 2
    When sequence 2 is received
    Then it is held as PENDING_ORDER
    And it is not processed
    When sequence 1 later arrives and is processed
    Then the buffered sequence 2 request is automatically released
    And it is processed next, in order

  @AC-05.3
  Scenario: A missing sequence is reported, not dropped
    Given a sequence number that never arrives within the batch
    When end-of-batch is reached
    Then the missing sequence is reported in the processing summary
    And it is not silently dropped
