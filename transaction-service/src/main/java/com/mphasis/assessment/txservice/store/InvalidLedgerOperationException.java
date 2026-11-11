package com.mphasis.assessment.txservice.store;

import com.mphasis.assessment.txservice.exception.TransactionServiceException;

/**
 * US-07: raised by {@link AccountLedger} when asked to apply a transaction
 * with data that must never reach the ledger - a bug upstream (the
 * pipeline should always validate and dedupe first), not a business
 * outcome to log and continue past.
 */
public class InvalidLedgerOperationException extends TransactionServiceException {

    private InvalidLedgerOperationException(String message) {
        super(message);
    }

    public static InvalidLedgerOperationException missingAccountId() {
        return new InvalidLedgerOperationException("accountId is required to apply a ledger entry");
    }

    public static InvalidLedgerOperationException missingTransactionType(String accountId) {
        return new InvalidLedgerOperationException("transactionType is required to apply a ledger entry for accountId=" + accountId);
    }

    public static InvalidLedgerOperationException invalidAmount(String accountId, Object amount) {
        return new InvalidLedgerOperationException("amount must be a positive number to apply a ledger entry for accountId=" + accountId + ", got: " + amount);
    }
}
