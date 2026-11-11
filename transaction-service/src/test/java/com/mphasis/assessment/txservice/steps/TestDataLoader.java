package com.mphasis.assessment.txservice.steps;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mphasis.assessment.txservice.model.TransactionRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Reads step test data from {@code testdata/<feature>/<fixture>.json} on
 * the test classpath (same classloader-resource pattern as
 * {@code InMemoryTransactionSource} for the real sample data), so a step's
 * actual request and expected-response values live in one JSON file
 * instead of being hand-typed into both the Gherkin sentence and the step
 * definition's Java code.
 */
final class TestDataLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestDataLoader() {
    }

    static JsonNode load(String classpathResourcePath) {
        try (InputStream in = TestDataLoader.class.getClassLoader().getResourceAsStream(classpathResourcePath)) {
            if (in == null) {
                throw new IllegalArgumentException("test data fixture not found on classpath: " + classpathResourcePath);
            }
            return MAPPER.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read test data fixture: " + classpathResourcePath, e);
        }
    }

    /** A fixture whose top level is either {"request": {...}, ...} or a bare request object. */
    static TransactionRequest loadRequest(String classpathResourcePath) {
        JsonNode root = load(classpathResourcePath);
        JsonNode requestNode = root.has("request") ? root.get("request") : root;
        return MAPPER.convertValue(requestNode, TransactionRequest.class);
    }

    /** A fixture whose top level is {"requests": [...]}. */
    static List<TransactionRequest> loadRequests(String classpathResourcePath) {
        JsonNode root = load(classpathResourcePath);
        return MAPPER.convertValue(root.get("requests"), new TypeReference<List<TransactionRequest>>() { });
    }

    /** The expected-response sub-object under the given key, e.g. "expectedValidation". */
    static JsonNode loadExpected(String classpathResourcePath, String key) {
        JsonNode node = load(classpathResourcePath).get(key);
        if (node == null) {
            throw new IllegalArgumentException("fixture " + classpathResourcePath + " has no \"" + key + "\" section");
        }
        return node;
    }
}
