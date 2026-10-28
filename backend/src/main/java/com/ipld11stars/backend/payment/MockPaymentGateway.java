package com.ipld11stars.backend.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Stand-in gateway for local development and tests: hands out ids and expects the matching
 * webhooks to be posted to /payments/webhook (signed with payments.webhook-secret).
 */
@Component
@ConditionalOnProperty(name = "payments.gateway", havingValue = "mock", matchIfMissing = true)
public class MockPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentGateway.class);

    @Override
    public String createOrder(String receiptId, BigDecimal amount, String currency) {
        String id = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        log.info("[mock gateway] order {} for receipt {} amount {} {}", id, receiptId, amount, currency);
        return id;
    }

    @Override
    public String createPayout(String receiptId, String userId, BigDecimal amount, String currency) {
        String id = "pout_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        log.info("[mock gateway] payout {} for receipt {} amount {} {}", id, receiptId, amount, currency);
        return id;
    }
}
