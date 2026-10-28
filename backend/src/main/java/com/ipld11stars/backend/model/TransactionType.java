package com.ipld11stars.backend.model;

/**
 * Kinds of wallet ledger entries. Credits are stored as positive amounts, debits as negative.
 */
public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    WITHDRAWAL_REVERSAL,
    ENTRY_FEE,
    ENTRY_REFUND,
    PRIZE
}
