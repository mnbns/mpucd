package com.mphasis.assessment.txservice.store;

import com.mphasis.assessment.txservice.model.TransactionType;
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

    public BigDecimal apply(String accountId, TransactionType type, BigDecimal amount) {
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
            log.debug("ledger update accountId={} type={}", mask(accountId), type);
            return updated;
        }
    }

    /** US-09 / NFR-06: never log the full account reference. */
    private static String mask(String accountId) {
        if (accountId == null || accountId.length() <= 4) {
            return "***";
        }
        return "***" + accountId.substring(accountId.length() - 3);
    }

    public BigDecimal getBalance(String accountId) {
        return balances.getOrDefault(accountId, BigDecimal.ZERO);
    }
}
