package com.ipld11stars.backend.repository.jdbc;

import com.ipld11stars.backend.model.PaymentOrder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class PaymentOrderRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<PaymentOrder> rowMapper = (rs, rowNum) -> {
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");
        return new PaymentOrder(rs.getString("id"), rs.getString("user_id"), rs.getString("direction"),
                rs.getBigDecimal("amount"), rs.getString("currency"), rs.getString("status"),
                rs.getString("gateway_order_id"), rs.getString("gateway_reference_id"), rs.getString("failure_reason"),
                created != null ? created.toLocalDateTime() : null, updated != null ? updated.toLocalDateTime() : null);
    };

    public PaymentOrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(PaymentOrder o) {
        jdbcTemplate.update("INSERT INTO ipld11stars.payment_orders (id, user_id, direction, amount, currency, status, " +
                        "gateway_order_id, gateway_reference_id, failure_reason, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                o.id(), o.userId(), o.direction(), o.amount(), o.currency(), o.status(), o.gatewayOrderId(),
                o.gatewayReferenceId(), o.failureReason());
    }

    public Optional<PaymentOrder> findById(String id) {
        return jdbcTemplate.query("SELECT * FROM ipld11stars.payment_orders WHERE id = ?", rowMapper, id).stream().findFirst();
    }

    /** Locks the order row for the rest of the transaction. */
    public Optional<PaymentOrder> lockByGatewayOrderId(String gatewayOrderId) {
        return jdbcTemplate.query("SELECT * FROM ipld11stars.payment_orders WHERE gateway_order_id = ? FOR UPDATE",
                rowMapper, gatewayOrderId).stream().findFirst();
    }

    public List<PaymentOrder> findByUserId(String userId) {
        return jdbcTemplate.query("SELECT * FROM ipld11stars.payment_orders WHERE user_id = ? ORDER BY created_at DESC, id",
                rowMapper, userId);
    }

    public void updateGatewayOrderId(String id, String gatewayOrderId) {
        jdbcTemplate.update("UPDATE ipld11stars.payment_orders SET gateway_order_id = ?, updated_at = NOW() WHERE id = ?",
                gatewayOrderId, id);
    }

    public void updateStatus(String id, String status, String gatewayReferenceId, String failureReason) {
        jdbcTemplate.update("UPDATE ipld11stars.payment_orders SET status = ?, " +
                        "gateway_reference_id = COALESCE(?, gateway_reference_id), failure_reason = ?, updated_at = NOW() WHERE id = ?",
                status, gatewayReferenceId, failureReason, id);
    }
}
