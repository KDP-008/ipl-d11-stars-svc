package com.ipld11stars.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One immutable wallet ledger entry. {@code amount} is signed; {@code balanceAfter} is the wallet
 * balance right after this entry was applied.
 */
public record WalletTransaction(String id, String userId, TransactionType type, BigDecimal amount,
                                BigDecimal balanceAfter, String referenceType, String referenceId,
                                String idempotencyKey, String description, LocalDateTime createdAt) {
}
