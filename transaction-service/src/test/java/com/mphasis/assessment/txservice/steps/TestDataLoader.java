package com.mphasis.assessment.txservice.steps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mphasis.assessment.txservice.model.TransactionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * definition's Java code. Every load is logged with the real JSON it read,
 * so the console/log file shows exactly what request body or expected
 * result a scenario used - directly comparable against the matching AC in
 * DESIGN.md, not inferred from the engine's internal state.
 */
final class TestDataLoader {

    private static final Logger log = LoggerFactory.getLogger(TestDataLoader.class);
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
        logJson("REQUEST BODY", classpathResourcePath, requestNode);
        return MAPPER.convertValue(requestNode, TransactionRequest.class);
    }

    /** A fixture whose top level is {"requests": [...]}. */
    static List<TransactionRequest> loadRequests(String classpathResourcePath) {
        JsonNode root = load(classpathResourcePath);
        JsonNode requestsNode = root.get("requests");
        logJson("REQUEST BODIES", classpathResourcePath, requestsNode);
        return MAPPER.convertValue(requestsNode, new TypeReference<List<TransactionRequest>>() { });
    }

    /** A resource whose top level is itself a bare array of requests - e.g. the real sample-transactions.json. */
    static List<TransactionRequest> loadRequestArray(String classpathResourcePath) {
        JsonNode root = load(classpathResourcePath);
        logJson("REQUEST BODIES", classpathResourcePath, root);
        return MAPPER.convertValue(root, new TypeReference<List<TransactionRequest>>() { });
    }

    /** The expected-response sub-object under the given key, e.g. "expectedValidation". */
    static JsonNode loadExpected(String classpathResourcePath, String key) {
        JsonNode node = load(classpathResourcePath).get(key);
        if (node == null) {
            throw new IllegalArgumentException("fixture " + classpathResourcePath + " has no \"" + key + "\" section");
        }
        logJson("EXPECTED (" + key + ")", classpathResourcePath, node);
        return node;
    }

    private static void logJson(String label, String classpathResourcePath, JsonNode node) {
        try {
            log.info("{} [{}]:\n{}", label, classpathResourcePath,
                    MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node));
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}
