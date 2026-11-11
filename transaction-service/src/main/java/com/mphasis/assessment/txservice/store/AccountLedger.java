package com.mphasis.assessment.txservice.store;

import com.mphasis.assessment.txservice.model.TransactionType;
import com.mphasis.assessment.txservice.util.AccountIdMasking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * US-07 / NFR-04: the actual banking balance each transaction affects.
 * Applying a transaction is the one side effect idempotency protects -
 * if the same requestId were applied twice, the balance would be wrong,
 * which is exactly what IdempotencyStore (checked before this is ever
 * called) exists to prevent.
 * Each account is updated under its own lock, so two transactions for
 * different accounts never block each other, but two transactions for
 * the same account are always serialized (AC-07.2).
 */
public class AccountLedger {

    private static final Logger log = LoggerFactory.getLogger(AccountLedger.class);

    private final Map<String, BigDecimal> balances = new ConcurrentHashMap<>();
    private final Map<String, Object> accountLocks = new ConcurrentHashMap<>();

    /**
     * @throws InvalidLedgerOperationException if accountId/type/amount is missing or amount
     *         isn't positive - the pipeline must validate and dedupe before ever reaching here,
     *         so this is a defensive guard against an upstream bug, not a business outcome.
     */
    public BigDecimal apply(String accountId, TransactionType type, BigDecimal amount) {
        if (accountId == null || accountId.isBlank()) {
            throw InvalidLedgerOperationException.missingAccountId();
        }
        if (type == null) {
            throw InvalidLedgerOperationException.missingTransactionType(accountId);
        }
        if (amount == null || amount.signum() <= 0) {
            throw InvalidLedgerOperationException.invalidAmount(accountId, amount);
        }

        Object lock = accountLocks.computeIfAbsent(accountId, id -> new Object());
        synchronized (lock) {
            BigDecimal current = balances.getOrDefault(accountId, BigDecimal.ZERO);
            BigDecimal updated = switch (type) {
                case CREDIT -> current.add(amount);
                case DEBIT, REVERSAL -> current.subtract(amount);
            };
            balances.put(accountId, updated);
            // Gap found comparing cucumber-tests.log against NFR-06 ("do not log sensitive
            // customer data unnecessarily"): amount/balance are financial data the rest of
            // this codebase never logs (TransactionProcessor.logEvent doesn't either) -
            // redacted here too for consistency, even though AC-08.2's own wording only
            // named accountId.
            log.debug("ledger update accountId={} type={}", AccountIdMasking.mask(accountId), type);
            return updated;
        }
    }

    public BigDecimal getBalance(String accountId) {
        return balances.getOrDefault(accountId, BigDecimal.ZERO);
    }
}
