@US-01
Feature: Configurable Transaction Ingestion
  Traces to: L2-FR-01

  As an operator
  I want the service to accept transaction requests from a configurable, replaceable input source
  So that the same core logic can later be wired to a real queue, file, or API without touching business logic

  @AC-01.1
  Scenario: Requests load through a single input source abstraction
    Given an in-memory list of transaction requests
    When the service starts
    Then it loads all requests through a single TransactionSource interface

  @AC-01.2
  Scenario: Swapping the input source does not affect business logic
    Given the input source implementation is swapped for a file reader
    When the service runs
    Then no change is required in the validator, processor, or state-store classes
