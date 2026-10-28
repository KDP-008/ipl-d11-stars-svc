package com.ipld11stars.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A deposit or withdrawal routed through the payment gateway.
 * Deposits: CREATED -> CAPTURED | FAILED. Withdrawals: PROCESSING -> PAID_OUT | REVERSED.
 */
public record PaymentOrder(String id, String userId, String direction, BigDecimal amount, String currency,
                           String status, String gatewayOrderId, String gatewayReferenceId, String failureReason,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static final String DEPOSIT = "DEPOSIT";
    public static final String WITHDRAWAL = "WITHDRAWAL";

    public static final String CREATED = "CREATED";
    public static final String CAPTURED = "CAPTURED";
    public static final String FAILED = "FAILED";
    public static final String PROCESSING = "PROCESSING";
    public static final String PAID_OUT = "PAID_OUT";
    public static final String REVERSED = "REVERSED";
}
