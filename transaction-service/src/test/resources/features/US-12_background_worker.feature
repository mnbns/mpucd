@US-12
Feature: Decoupled Background Processing Worker
  Traces to: L2-FR-13

  As an operator
  I want ingestion and processing to run as separate producer/consumer roles
  So that the service has a real background-processing component, not just a
  loop that happens to call the processor synchronously

  @AC-12.1
  Scenario: A submitted request is queued, not processed inline by the ingestion loop
    Given a background worker wired to the scenario's transaction processor
    When requestId "REQ-W1" is submitted to the background worker before it is started
    Then requestId "REQ-W1" has not been touched by processing yet
    When the background worker is started and allowed to drain
    Then requestId "REQ-W1" has final status "PROCESSED"

  @AC-12.2
  Scenario: The ingesting thread is not blocked, and the summary is only trustworthy once the worker drains
    Given a background worker wired to the scenario's transaction processor
    When 3 valid requests in sequence are submitted to the background worker before it is started
    Then none of the submitted requests have been touched by processing yet
    When the background worker is started and allowed to drain
    And the processing summary is generated
    Then the summary counts exactly 3 PROCESSED, 0 DUPLICATE, and 0 FAILED

  @AC-12.3
  Scenario: The worker drains strictly in arrival order through a single consumer
    Given a background worker wired to the scenario's transaction processor
    And two sequence-dependent requests for the same account, as in section 9.8 rows 1 and 2, loaded from "testdata/US-05_out_of_order_handling/txn2002_before_txn2001_section_9_8.json"
    When they are submitted to the background worker in their documented arrival order, before it is started
    And the background worker is started and allowed to drain
    Then requestId "6820394" has final status "PROCESSED"
    And requestId "4193857" has final status "PROCESSED"
    And the background worker used exactly one consumer thread
