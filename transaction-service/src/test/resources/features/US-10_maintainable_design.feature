@US-10
Feature: Maintainable, Modular Design
  Traces to: L2-FR-10, NFR-07

  As a future maintainer
  I want business rules separated from input/infrastructure code
  So that the service can evolve, such as swapping the in-memory store for a real database, with minimal risk

  @AC-10.1
  Scenario: Processor has no dependency on input source details
    Given the TransactionProcessor business logic
    When it is inspected
    Then it has no direct dependency on how input is sourced
    And it depends only on a TransactionSource interface

  @AC-10.2
  Scenario: Idempotency store is swappable behind an interface
    Given the idempotency store
    When it is inspected
    Then it sits behind an IdempotencyStore interface
    And an in-memory map can later be swapped for Redis or a database without touching business logic
