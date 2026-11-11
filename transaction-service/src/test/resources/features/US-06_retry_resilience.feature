@US-06
Feature: Retry-Safe Resilience
  Traces to: L2-FR-08, L2-FR-09, NFR-03

  As an operator
  I want transient failures retried automatically without duplicating effects
  So that temporary downstream issues never cause lost or duplicated transactions

  @AC-06.1
  Scenario: Transient failure moves a transaction to RETRY_PENDING
    Given a transaction simulated to fail transiently, such as TXN-2005 in section 9.8 row 6
    When processing fails
    Then status becomes RETRY_PENDING
    And retryCount is incremented

  @AC-06.2
  Scenario: Exhausted retries result in FAILED with no partial effect
    Given a transaction retried up to the configured max retry count and still failing
    When the last retry fails
    Then status becomes FAILED with a recorded reason
    And no partial effect remains

  @AC-06.3
  Scenario: Eventual success after retry is recorded exactly once
    Given a transaction that succeeds on a later retry attempt
    When it succeeds
    Then it is marked PROCESSED exactly once
    And the idempotency store prevents any earlier partial attempt from also taking effect
