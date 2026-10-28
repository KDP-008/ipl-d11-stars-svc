package com.ipld11stars.backend.repository.jdbc;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PaymentWebhookEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public PaymentWebhookEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Records the event id; returns false if this event was already received. */
    public boolean tryInsert(String eventId, String eventType, String gatewayOrderId, String payload) {
        try {
            jdbcTemplate.update("INSERT INTO ipld11stars.payment_webhook_events (event_id, event_type, gateway_order_id, outcome, " +
                    "payload, received_at) VALUES (?, ?, ?, 'RECEIVED', ?, NOW())", eventId, eventType, gatewayOrderId, payload);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    public void updateOutcome(String eventId, String outcome) {
        jdbcTemplate.update("UPDATE ipld11stars.payment_webhook_events SET outcome = ? WHERE event_id = ?", outcome, eventId);
    }

    public List<String> findOutcomes(String gatewayOrderId) {
        return jdbcTemplate.queryForList("SELECT outcome FROM ipld11stars.payment_webhook_events WHERE gateway_order_id = ? " +
                "ORDER BY received_at, event_id", String.class, gatewayOrderId);
    }
}
