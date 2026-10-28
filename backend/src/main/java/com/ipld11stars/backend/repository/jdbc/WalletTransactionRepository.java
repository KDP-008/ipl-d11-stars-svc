package com.ipld11stars.backend.repository.jdbc;

import com.ipld11stars.backend.model.TransactionType;
import com.ipld11stars.backend.model.WalletTransaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class WalletTransactionRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<WalletTransaction> rowMapper = (rs, rowNum) -> {
        Timestamp created = rs.getTimestamp("created_at");
        return new WalletTransaction(rs.getString("id"), rs.getString("user_id"),
                TransactionType.valueOf(rs.getString("txn_type")), rs.getBigDecimal("amount"),
                rs.getBigDecimal("balance_after"), rs.getString("reference_type"), rs.getString("reference_id"),
                rs.getString("idempotency_key"), rs.getString("description"),
                created != null ? created.toLocalDateTime() : null);
    };

    public WalletTransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<WalletTransaction> findByIdempotencyKey(String key) {
        return jdbcTemplate.query("SELECT * FROM ipld11stars.wallet_transactions WHERE idempotency_key = ?", rowMapper, key)
                .stream().findFirst();
    }

    public List<WalletTransaction> findByUserId(String userId) {
        return jdbcTemplate.query("SELECT * FROM ipld11stars.wallet_transactions WHERE user_id = ? ORDER BY created_at DESC, id DESC",
                rowMapper, userId);
    }

    public void insert(WalletTransaction t) {
        jdbcTemplate.update("INSERT INTO ipld11stars.wallet_transactions (id, user_id, txn_type, amount, balance_after, " +
                        "reference_type, reference_id, idempotency_key, description, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                t.id(), t.userId(), t.type().name(), t.amount(), t.balanceAfter(), t.referenceType(), t.referenceId(),
                t.idempotencyKey(), t.description());
    }
}
