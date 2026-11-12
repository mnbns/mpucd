# Unit Test Execution Evidence

Raw JUnit/Maven Surefire output for the **unit test suite** (`src/test/java/.../unit/`),
as distinct from the Cucumber/BDD scenarios covered in `Test Evidence.html`. Generated
by `mvn test` from `transaction-service/`; raw files live at
`transaction-service/target/surefire-reports/`.

## Summary

| Test class | Tests | Failures | Errors | Skipped | Time (s) |
|---|---|---|---|---|---|
| `InMemoryTransactionSourceTest` | 6 | 0 | 0 | 0 | 1.640 |
| `TransactionRequestTest` | 14 | 0 | 0 | 0 | 0.052 |
| `SequenceBufferTest` | 8 | 0 | 0 | 0 | 0.051 |
| `RetryPolicyTest` | 4 | 0 | 0 | 0 | 0.081 |
| `RequestSchemaValidatorTest` | 18 | 0 | 0 | 0 | 0.102 |
| `SchemaValidationResultTest` | 2 | 0 | 0 | 0 | 0.004 |
| `ProcessingSummaryTest` | 3 | 0 | 0 | 0 | 0.012 |
| `TransactionProcessorTest` | 16 | 0 | 0 | 0 | 0.169 |
| `ValidationResultTest` | 2 | 0 | 0 | 0 | 0.006 |
| `ValidatorTest` | 10 | 0 | 0 | 0 | 0.093 |
| `AccountLedgerTest` | 12 | 0 | 0 | 0 | 0.075 |
| `InMemoryIdempotencyStoreTest` | 4 | 0 | 0 | 0 | 0.013 |
| `AccountIdMaskingTest` | 4 | 0 | 0 | 0 | 0.011 |
| **Total (unit)** | **103** | **0** | **0** | **0** | — |

Combined with the 39 Cucumber scenarios (`RunCucumberTest`, also run under Surefire,
0 failures), the full `mvn test` run reports **142 tests, 0 failures, 0 errors** —
`BUILD SUCCESS`.

## Raw Surefire output (`target/surefire-reports/*.txt`)

```
-------------------------------------------------------------------------------
Test set: com.mphasis.assessment.txservice.unit.input.InMemoryTransactionSourceTest
-------------------------------------------------------------------------------
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.640 s -- in com.mphasis.assessment.txservice.unit.input.InMemoryTransactionSourceTest

-------------------------------------------------------------------------------
Test set: com.mphasis.assessment.txservice.unit.model.TransactionRequestTest
-------------------------------------------------------------------------------
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.052 s -- in com.mphasis.assessment.txservice.unit.model.TransactionRequestTest

-------------------------------------------------------------------------------
Test set: com.mphasis.assessment.txservice.unit.ordering.SequenceBufferTest
-------------------------------------------------------------------------------
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.051 s -- in com.mphasis.assessment.txservice.unit.ordering.SequenceBufferTest

-------------------------------------------------------------------------------
Test set: com.mphasis.assessment.txservice.unit.retry.RetryPolicyTest
-------------------------------------------------------------------------------
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.081 s -- in com.mphasis.assessment.txservice.unit.retry.RetryPolicyTest

-------------------------------------------------------------------------------
Test set: com.mphasis.assessment.txservice.unit.schema.RequestSchemaValidatorTest
-------------------------------------------------------------------------------
Tests run: 18, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.102 s -- in com.mphasis.assessment.txservice.unit.schema.RequestSchemaValidatorTest

-------------------------------------------------------------------------------
Test set: com.mphasis.assessment.txservice.unit.schema.SchemaValidationResultTest
-------------------------------------------------------------------------------
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.mphasis.assessment.txservice.unit.schema.SchemaValidationResultTest

-------------------------------------------------------------------------------
Test set: com.mphasis.assessment.txservice.unit.service.ProcessingSummaryTest
-------------------------------------------------------------------------------
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.012 s -- in com.mphasis.assessment.txservice.unit.service.ProcessingSummaryTest

-------------------------------------------------------------------------------
Test set: com.mphasis.assessment.txservice.unit.service.TransactionProcessorTest
-------------------------------------------------------------------------------
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.169 s -- in com.mphasis.assessment.txservice.unit.service.TransactionProcessorTest

-------------------------------------------------------------------------------
Test set: com.mphasis.assessment.txservice.unit.service.ValidationResultTest
-------------------------------------------------------------------------------
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.mphasis.assessment.txservice.unit.service.ValidationResultTest

-------------------------------------------------------------------------------
Test set: com.mphasis.assessment.txservice.unit.service.ValidatorTest
-------------------------------------------------------------------------------
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.093 s -- in com.mphasis.assessment.txservice.unit.service.ValidatorTest

-------------------------------------------------------------------------------
Test set: com.mphasis.assessment.txservice.unit.store.AccountLedgerTest
-------------------------------------------------------------------------------
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.075 s -- in com.mphasis.assessment.txservice.unit.store.AccountLedgerTest

-------------------------------------------------------------------------------
Test set: com.mphasis.assessment.txservice.unit.store.InMemoryIdempotencyStoreTest
-------------------------------------------------------------------------------
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.013 s -- in com.mphasis.assessment.txservice.unit.store.InMemoryIdempotencyStoreTest

-------------------------------------------------------------------------------
Test set: com.mphasis.assessment.txservice.unit.util.AccountIdMaskingTest
-------------------------------------------------------------------------------
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.011 s -- in com.mphasis.assessment.txservice.unit.util.AccountIdMaskingTest

-------------------------------------------------------------------------------
Test set: com.mphasis.assessment.txservice.RunCucumberTest
-------------------------------------------------------------------------------
Tests run: 39, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.226 s -- in com.mphasis.assessment.txservice.RunCucumberTest
```

## How to regenerate

```bash
cd transaction-service
mvn test
```

Fresh `.txt`/`.xml` reports land in `target/surefire-reports/` (one file pair per test
class), and the BDD-specific HTML/JSON reports land in `target/cucumber-reports/`
(see `Test Evidence.html` for the committed snapshot of that report).
