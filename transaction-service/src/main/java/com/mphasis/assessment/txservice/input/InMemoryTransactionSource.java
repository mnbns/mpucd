package com.mphasis.assessment.txservice.input;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mphasis.assessment.txservice.model.TransactionRequest;

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
            return objectMapper.readValue(in, new TypeReference<List<TransactionRequest>>() { });
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + resourcePath, e);
        }
    }
}
