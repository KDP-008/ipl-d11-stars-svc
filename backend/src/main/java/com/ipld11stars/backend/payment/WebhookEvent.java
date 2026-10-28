package com.ipld11stars.backend.payment;

import java.math.BigDecimal;

/**
 * Parsed gateway webhook.
 * Shape: {"id": "evt_..", "type": "payment.captured", "data": {"orderId": "...", "referenceId": "...",
 * "amount": "500.00", "reason": "..."}}. orderId is our gateway_order_id (order or payout id).
 */
public record WebhookEvent(String id, String type, Data data) {

    public record Data(String orderId, String referenceId, BigDecimal amount, String reason) {
    }

    public static final String PAYMENT_CAPTURED = "payment.captured";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String PAYOUT_PROCESSED = "payout.processed";
    public static final String PAYOUT_REVERSED = "payout.reversed";
    public static final String PAYOUT_FAILED = "payout.failed";
}
