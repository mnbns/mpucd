@US-03
Feature: Transaction State Lifecycle
  Traces to: L2-FR-03, L2-FR-04

  As a QA / operations reviewer
  I want every transaction to carry a well-defined, enforced status
  So that I can always tell what happened to a given request without reading code

  @AC-03.1
  Scenario: New request starts as RECEIVED
    Given a new request
    When it is constructed, before entering the processing pipeline
    Then its initial status is RECEIVED

  @AC-03.2
  Scenario: Processing transitions status to PROCESSING
    Given a RECEIVED request picked up for work
    When processing starts
    Then status transitions to PROCESSING

  @AC-03.3
  Scenario: Status is always one of the defined lifecycle values
    Given any transaction at any point in time
    When its status is inspected
    Then it is exactly one of RECEIVED, PROCESSING, PENDING_ORDER, RETRY_PENDING, PROCESSED, DUPLICATE, or FAILED

  @AC-03.4
  Scenario: Illegal transitions are rejected
    Given an illegal transition is attempted, such as DUPLICATE to PROCESSING
    When it is attempted
    Then it is rejected and logged as an invalid transition
