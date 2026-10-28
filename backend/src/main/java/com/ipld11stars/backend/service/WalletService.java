package com.ipld11stars.backend.service;

import com.ipld11stars.backend.exception.BusinessRuleException;
import com.ipld11stars.backend.model.TransactionType;
import com.ipld11stars.backend.model.WalletTransaction;
import com.ipld11stars.backend.repository.jdbc.WalletRepository;
import com.ipld11stars.backend.repository.jdbc.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * User wallets backed by an append-only ledger.
 *
 * Every balance change goes through {@link #apply}, which locks the wallet row, checks the
 * idempotency key under that lock, and writes the ledger entry together with the new balance.
 * Replaying the same key returns the original entry without moving money again.
 */
@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    public WalletService(WalletRepository walletRepository, WalletTransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    public BigDecimal getBalance(String userId) {
        return walletRepository.findBalance(userId);
    }

    public List<WalletTransaction> getTransactions(String userId) {
        return transactionRepository.findByUserId(userId);
    }

    @Transactional
    public WalletTransaction credit(String userId, BigDecimal amount, TransactionType type, String referenceType,
                                    String referenceId, String idempotencyKey, String description) {
        return apply(userId, requirePositive(amount), type, referenceType, referenceId, idempotencyKey, description);
    }

    @Transactional
    public WalletTransaction debit(String userId, BigDecimal amount, TransactionType type, String referenceType,
                                   String referenceId, String idempotencyKey, String description) {
        return apply(userId, requirePositive(amount).negate(), type, referenceType, referenceId, idempotencyKey, description);
    }

    private WalletTransaction apply(String userId, BigDecimal signedAmount, TransactionType type, String referenceType,
                                    String referenceId, String idempotencyKey, String description) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey is required for wallet movements");
        }
        walletRepository.ensureExists(userId);
        BigDecimal balance = walletRepository.lockBalance(userId);

        WalletTransaction existing = transactionRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            if (!existing.userId().equals(userId) || existing.amount().compareTo(signedAmount) != 0) {
                throw BusinessRuleException.conflict("Idempotency key " + idempotencyKey + " was already used for a different movement");
            }
            return existing;
        }

        BigDecimal newBalance = balance.add(signedAmount);
        if (newBalance.signum() < 0) {
            throw BusinessRuleException.conflict("Insufficient wallet balance");
        }
        walletRepository.updateBalance(userId, newBalance);
        WalletTransaction txn = new WalletTransaction(UUID.randomUUID().toString(), userId, type, signedAmount,
                newBalance, referenceType, referenceId, idempotencyKey, description, null);
        transactionRepository.insert(txn);
        return txn;
    }

    private static BigDecimal requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("Amount must be greater than zero");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new BusinessRuleException("Amounts can have at most two decimal places");
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }
}
