package com.mphasis.assessment.txservice.input;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mphasis.assessment.txservice.model.TransactionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * US-01 AC-01.1: loads the §9.8 sample batch from a bundled JSON resource,
 * preserving its array order as the "Input Order" column (no separate
 * ordering field needed - the file order IS the input order).
 */
public class InMemoryTransactionSource implements TransactionSource {

    private static final Logger log = LoggerFactory.getLogger(InMemoryTransactionSource.class);

    private final String resourcePath;
    private final ObjectMapper objectMapper;

    public InMemoryTransactionSource(String resourcePath, ObjectMapper objectMapper) {
        this.resourcePath = resourcePath;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<TransactionRequest> fetchBatch() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Sample data resource not found on classpath: " + resourcePath);
            }
            List<TransactionRequest> batch = objectMapper.readValue(in, new TypeReference<List<TransactionRequest>>() { });
            log.info("loaded {} transaction request(s) from {}", batch.size(), resourcePath);
            return batch;
        } catch (IOException e) {
            log.error("failed to read transaction batch from {}", resourcePath, e);
            throw new UncheckedIOException("Failed to read " + resourcePath, e);
        }
    }
}
