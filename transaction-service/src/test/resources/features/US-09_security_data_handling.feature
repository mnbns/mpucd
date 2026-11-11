@US-09
Feature: Security and Safe Data Handling
  Traces to: NFR-06

  As a security-conscious engineer
  I want sensitive fields protected and all input validated
  So that the service never leaks data or accepts unsafe input

  @AC-09.1
  Scenario: Logs never contain full account numbers or PII
    Given any log statement for a transaction
    When it is emitted
    Then it never includes full account numbers or PII beyond a masked or short accountId reference

  @AC-09.2
  Scenario: No hardcoded secrets in configuration
    Given the service configuration
    When it is reviewed
    Then no secrets or credentials are hardcoded in source
    And all are externalized
