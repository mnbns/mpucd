package com.mphasis.assessment.txservice.unit.store;

import com.mphasis.assessment.txservice.model.TransactionType;
import com.mphasis.assessment.txservice.store.AccountLedger;
import com.mphasis.assessment.txservice.store.InvalidLedgerOperationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for {@link AccountLedger}: balance math per type, per-account isolation, and the defensive guards. */
class AccountLedgerTest {

    private final AccountLedger ledger = new AccountLedger();

    @Test
    void unknownAccountDefaultsToZeroBalance() {
        assertEquals(BigDecimal.ZERO, ledger.getBalance("ACC-UNKNOWN"));
    }

    @Test
    void creditIncreasesBalance() {
        ledger.apply("ACC-1", TransactionType.CREDIT, new BigDecimal("100"));
        assertEquals(new BigDecimal("100"), ledger.getBalance("ACC-1"));
    }

    @Test
    void debitDecreasesBalance() {
        ledger.apply("ACC-1", TransactionType.CREDIT, new BigDecimal("100"));
        ledger.apply("ACC-1", TransactionType.DEBIT, new BigDecimal("40"));
        assertEquals(new BigDecimal("60"), ledger.getBalance("ACC-1"));
    }

    @Test
    void reversalDecreasesBalanceLikeADebit() {
        ledger.apply("ACC-1", TransactionType.CREDIT, new BigDecimal("100"));
        ledger.apply("ACC-1", TransactionType.REVERSAL, new BigDecimal("30"));
        assertEquals(new BigDecimal("70"), ledger.getBalance("ACC-1"));
    }

    @Test
    void sequentialApplicationsAccumulate() {
        ledger.apply("ACC-1", TransactionType.CREDIT, new BigDecimal("10"));
        ledger.apply("ACC-1", TransactionType.CREDIT, new BigDecimal("20"));
        ledger.apply("ACC-1", TransactionType.CREDIT, new BigDecimal("30"));
        assertEquals(new BigDecimal("60"), ledger.getBalance("ACC-1"));
    }

    @Test
    void differentAccountsDoNotInterfere() {
        ledger.apply("ACC-1", TransactionType.CREDIT, new BigDecimal("100"));
        ledger.apply("ACC-2", TransactionType.CREDIT, new BigDecimal("5"));
        assertEquals(new BigDecimal("100"), ledger.getBalance("ACC-1"));
        assertEquals(new BigDecimal("5"), ledger.getBalance("ACC-2"));
    }

    @Test
    void nullAccountIdThrows() {
        InvalidLedgerOperationException ex = assertThrows(InvalidLedgerOperationException.class,
                () -> ledger.apply(null, TransactionType.CREDIT, BigDecimal.TEN));
        assertTrue(ex.getMessage().contains("accountId is required"));
    }

    @Test
    void blankAccountIdThrows() {
        assertThrows(InvalidLedgerOperationException.class,
                () -> ledger.apply("  ", TransactionType.CREDIT, BigDecimal.TEN));
    }

    @Test
    void nullTransactionTypeThrows() {
        InvalidLedgerOperationException ex = assertThrows(InvalidLedgerOperationException.class,
                () -> ledger.apply("ACC-1", null, BigDecimal.TEN));
        assertTrue(ex.getMessage().contains("transactionType is required"));
    }

    @Test
    void nullAmountThrows() {
        assertThrows(InvalidLedgerOperationException.class,
                () -> ledger.apply("ACC-1", TransactionType.CREDIT, null));
    }

    @Test
    void zeroOrNegativeAmountThrows() {
        assertThrows(InvalidLedgerOperationException.class,
                () -> ledger.apply("ACC-1", TransactionType.CREDIT, BigDecimal.ZERO));
        assertThrows(InvalidLedgerOperationException.class,
                () -> ledger.apply("ACC-1", TransactionType.CREDIT, new BigDecimal("-5")));
    }

    @Test
    void aRejectedApplyLeavesTheBalanceUntouched() {
        ledger.apply("ACC-1", TransactionType.CREDIT, new BigDecimal("50"));
        try {
            ledger.apply("ACC-1", TransactionType.CREDIT, new BigDecimal("-1"));
        } catch (InvalidLedgerOperationException expected) {
            // ignored - asserting the side effect below
        }
        assertEquals(new BigDecimal("50"), ledger.getBalance("ACC-1"));
    }
}
