@US-07
Feature: Data Consistency
  Traces to: NFR-04

  As a system owner
  I want transaction processing to be atomic from the caller's point of view
  So that no transaction is ever left half-applied

  @AC-07.1
  Scenario: A failure mid-step never leaves a mixed state
    Given a transaction being processed
    When a failure occurs mid-step
    Then the visible state is either fully PROCESSED or fully FAILED or RETRY_PENDING
    And it is never a mix of updated-but-unmarked state

  @AC-07.2
  Scenario: Concurrent requests for the same account are serialized
    Given concurrent requests for the same accountId
    When both are processed
    Then updates are serialized per account to avoid lost updates
