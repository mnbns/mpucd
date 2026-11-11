@US-08
Feature: Observability and Processing Summary
  Traces to: L2-FR-11, NFR-05

  As an SDET / operations reviewer
  I want structured logs and a summary report at the end of a run
  So that I can verify outcomes without manually inspecting code

  @AC-08.1
  Scenario: Summary reports counts by status
    Given a batch run has completed
    When the summary is generated
    Then it reports counts by status for RECEIVED, PROCESSED, DUPLICATE, FAILED, RETRY_PENDING, and PENDING_ORDER

  @AC-08.2
  Scenario: Every event is logged without sensitive fields
    Given any transaction event such as received, duplicate, retried, or failed
    When it occurs
    Then a structured log line is emitted with transactionId, requestId, status, and timestamp
    And it contains no sensitive fields
