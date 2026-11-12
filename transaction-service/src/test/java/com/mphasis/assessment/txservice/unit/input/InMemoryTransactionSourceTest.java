package com.mphasis.assessment.txservice.unit.input;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mphasis.assessment.txservice.input.InMemoryTransactionSource;
import com.mphasis.assessment.txservice.input.TransactionSourceException;
import com.mphasis.assessment.txservice.model.TransactionRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link InMemoryTransactionSource}: classpath loading, schema-validation
 * filtering of malformed records (L2-FR-02 hardening), and the resource/malformed-batch
 * failure paths. Uses dedicated fixtures under {@code unit-fixtures/input} (kept out of
 * the Cucumber-owned {@code testdata} tree) plus the real bundled sample-transactions.json.
 */
class InMemoryTransactionSourceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void loadsTheRealBundledSampleBatchPreservingFileOrder() {
        InMemoryTransactionSource source = new InMemoryTransactionSource("sample-transactions.json", mapper);
        List<TransactionRequest> batch = source.fetchBatch();

        assertEquals(6, batch.size());
        assertEquals("REQ-2", batch.get(0).getRequestId()); // file order preserved, not sorted by sequence
        assertEquals("REQ-1", batch.get(1).getRequestId());
    }

    @Test
    void loadsAllRecordsFromAWellFormedBatch() {
        InMemoryTransactionSource source = new InMemoryTransactionSource(
                "unit-fixtures/input/valid_batch.json", mapper);
        List<TransactionRequest> batch = source.fetchBatch();
        assertEquals(2, batch.size());
        assertEquals("REQ-U001", batch.get(0).getRequestId());
        assertEquals("REQ-U002", batch.get(1).getRequestId());
    }

    @Test
    void rejectsOnlyTheMalformedRecordAndKeepsTheRest() {
        InMemoryTransactionSource source = new InMemoryTransactionSource(
                "unit-fixtures/input/batch_with_one_invalid_record.json", mapper);
        List<TransactionRequest> batch = source.fetchBatch();

        // the record with a blank requestId fails RequestSchemaValidator and is excluded,
        // but it does not crash or exclude the valid record alongside it
        assertEquals(1, batch.size());
        assertEquals("REQ-U010", batch.get(0).getRequestId());
    }

    @Test
    void resourceNotFoundOnClasspathThrowsTransactionSourceException() {
        InMemoryTransactionSource source = new InMemoryTransactionSource(
                "unit-fixtures/input/does-not-exist.json", mapper);
        TransactionSourceException ex = assertThrows(TransactionSourceException.class, source::fetchBatch);
        assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    void nonArrayTopLevelThrowsTransactionSourceExceptionForMalformedBatch() {
        InMemoryTransactionSource source = new InMemoryTransactionSource(
                "unit-fixtures/input/not_an_array.json", mapper);
        TransactionSourceException ex = assertThrows(TransactionSourceException.class, source::fetchBatch);
        assertTrue(ex.getMessage().contains("Malformed batch"));
    }

    @Test
    void duplicateJsonKeyInARecordFailsTheWholeBatchRead() {
        // STRICT_DUPLICATE_DETECTION: a repeated JSON key is malformed JSON, not a business
        // duplicate transactionId - it must fail the whole source read, not just one record.
        InMemoryTransactionSource source = new InMemoryTransactionSource(
                "unit-fixtures/input/duplicate_json_key.json", mapper);
        assertThrows(TransactionSourceException.class, source::fetchBatch);
    }
}
