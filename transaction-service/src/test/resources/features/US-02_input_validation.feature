@US-02
Feature: Input Validation
  Traces to: L2-FR-02, NFR-06

  As a system owner
  I want malformed transaction records rejected before processing
  So that bad data cannot corrupt transaction state or crash the batch

  @AC-02.1
  Scenario Outline: Missing mandatory field is rejected
    Given a request missing a mandatory field such as transactionId, requestId, accountId, amount, or type, loaded from "<fixture>"
    When it is ingested
    Then it is marked FAILED with a validation reason
    And it is excluded from business processing

    Examples:
      | missingField    | fixture                                                       |
      | accountId       | testdata/US-02_input_validation/missing_mandatory_field.json  |
      | transactionId   | testdata/US-02_input_validation/missing_transactionId.json    |
      | requestId       | testdata/US-02_input_validation/missing_requestId.json        |
      | currency        | testdata/US-02_input_validation/missing_currency.json         |
      | transactionType | testdata/US-02_input_validation/missing_transactionType.json  |

  @AC-02.2
  Scenario Outline: Invalid amount is rejected with a clear reason
    Given a request with a negative amount that violates the field rules, loaded from "<fixture>"
    When it is validated
    Then it is rejected as FAILED with a clear reason
    And the rest of the batch still processes

    Examples:
      | case            | fixture                                                |
      | negative amount | testdata/US-02_input_validation/negative_amount.json   |
      | zero amount     | testdata/US-02_input_validation/zero_amount.json       |

  @AC-02.3
  Scenario: One invalid record does not stop the batch
    Given one invalid record inside a batch of N records, loaded from "testdata/US-02_input_validation/one_invalid_in_batch.json"
    When the batch runs
    Then the remaining N-1 valid records are still processed normally

  @AC-02.4
  Scenario: Non-positive sequence number is rejected
    Given a request with a non-positive sequence number that violates the field rules, loaded from "testdata/US-02_input_validation/invalid_sequence_number.json"
    When it is validated
    Then it is rejected as FAILED with a clear reason
    And the rest of the batch still processes
