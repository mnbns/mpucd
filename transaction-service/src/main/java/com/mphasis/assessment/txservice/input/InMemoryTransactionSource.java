package com.mphasis.assessment.txservice.input;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mphasis.assessment.txservice.model.TransactionRequest;
import com.mphasis.assessment.txservice.schema.RequestSchemaValidator;
import com.mphasis.assessment.txservice.schema.SchemaValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * US-01 AC-01.1: loads the §9.8 sample batch from a bundled JSON resource,
 * preserving its array order as the "Input Order" column (no separate
 * ordering field needed - the file order IS the input order).
 * <p>
 * L2-FR-02 hardening: every record is checked against {@link RequestSchemaValidator}
 * before it is deserialized - a record that fails (wrong/missing/extra fields,
 * malformed id shapes) is logged and excluded rather than crashing the whole
 * batch load, the same "one bad record doesn't stop the rest" principle as
 * AC-02.3, applied one layer earlier than the field-level {@code Validator}.
 */
public class InMemoryTransactionSource implements TransactionSource {

    private static final Logger log = LoggerFactory.getLogger(InMemoryTransactionSource.class);

    private final String resourcePath;
    private final ObjectMapper objectMapper;
    private final RequestSchemaValidator schemaValidator = new RequestSchemaValidator();

    public InMemoryTransactionSource(String resourcePath, ObjectMapper objectMapper) {
        this.resourcePath = resourcePath;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<TransactionRequest> fetchBatch() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw TransactionSourceException.resourceNotFound(resourcePath);
            }

            // STRICT_DUPLICATE_DETECTION: a record with a repeated JSON key (e.g.
            // {"amount": 10, "amount": 20}) is genuinely malformed JSON, not a
            // business-level duplicate transactionId (which is intentionally valid
            // input, exercised by US-04's dedup tests) - reject the whole malformed
            // source rather than guessing which occurrence was meant.
            JsonNode root = objectMapper.reader()
                    .with(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                    .readTree(in);
            if (root == null || !root.isArray()) {
                throw TransactionSourceException.malformedBatch(resourcePath, "expected a JSON array of requests");
            }

            List<TransactionRequest> batch = new ArrayList<>();
            int rejected = 0;
            for (JsonNode recordNode : root) {
                try {
                    schemaValidator.validateOrThrow(recordNode);
                    batch.add(objectMapper.treeToValue(recordNode, TransactionRequest.class));
                } catch (SchemaValidationException | IOException e) {
                    rejected++;
                    log.warn("rejected malformed JSON record from {} (schema validation failed): {}", resourcePath, e.getMessage());
                }
            }

            log.info("loaded {} transaction request(s) from {} ({} rejected at schema validation)",
                    batch.size(), resourcePath, rejected);
            return batch;
        } catch (IOException e) {
            log.error("failed to read transaction batch from {}", resourcePath, e);
            throw TransactionSourceException.readFailed(resourcePath, e);
        }
    }
}
