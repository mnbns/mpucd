package com.mphasis.assessment.txservice.util;

/**
 * US-09 / NFR-06: the one place that decides how an accountId is masked
 * for logs, so {@link com.mphasis.assessment.txservice.service.TransactionProcessor}
 * and {@link com.mphasis.assessment.txservice.store.AccountLedger} can't drift
 * out of sync on what "masked" means.
 */
public final class AccountIdMasking {

    private AccountIdMasking() {
    }

    public static String mask(String accountId) {
        if (accountId == null || accountId.length() <= 4) {
            return "***";
        }
        return "***" + accountId.substring(accountId.length() - 3);
    }
}
