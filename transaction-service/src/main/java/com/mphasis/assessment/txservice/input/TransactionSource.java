package com.mphasis.assessment.txservice.input;

import com.mphasis.assessment.txservice.model.TransactionRequest;

import java.util.List;

/**
 * US-01 / L2-FR-01: the one seam the processing engine depends on for input.
 * Swap the implementation (file, queue, API adapter) without touching
 * Validator, TransactionProcessor, or any store.
 */
public interface TransactionSource {
    List<TransactionRequest> fetchBatch();
}
