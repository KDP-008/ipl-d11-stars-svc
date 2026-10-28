package com.ipld11stars.backend.repository.jdbc;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class WalletRepository {

    private final JdbcTemplate jdbcTemplate;

    public WalletRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public BigDecimal findBalance(String userId) {
        List<BigDecimal> rows = jdbcTemplate.queryForList(
                "SELECT balance FROM ipld11stars.wallets WHERE user_id = ?", BigDecimal.class, userId);
        return rows.isEmpty() ? BigDecimal.ZERO.setScale(2) : rows.get(0);
    }

    /** Creates an empty wallet if the user has none yet. Safe to call concurrently. */
    public void ensureExists(String userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ipld11stars.wallets WHERE user_id = ?", Integer.class, userId);
        if (count != null && count > 0) {
            return;
        }
        try {
            jdbcTemplate.update("INSERT INTO ipld11stars.wallets (user_id, balance, created_at, updated_at) VALUES (?, 0.00, NOW(), NOW())",
                    userId);
        } catch (DuplicateKeyException ignored) {
            // created by a concurrent request
        }
    }

    /** Reads the balance and locks the wallet row until the surrounding transaction ends. */
    public BigDecimal lockBalance(String userId) {
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM ipld11stars.wallets WHERE user_id = ? FOR UPDATE", BigDecimal.class, userId);
    }

    public void updateBalance(String userId, BigDecimal balance) {
        jdbcTemplate.update("UPDATE ipld11stars.wallets SET balance = ?, updated_at = NOW() WHERE user_id = ?", balance, userId);
    }
}
