package com.ipld11stars.backend.payment;

import java.math.BigDecimal;

/**
 * Outbound calls to the payment provider. Results of both calls arrive later as signed webhooks.
 */
public interface PaymentGateway {

    /** Creates a checkout order for a wallet top-up and returns the provider's order id. */
    String createOrder(String receiptId, BigDecimal amount, String currency);

    /** Starts a bank payout for a withdrawal and returns the provider's payout id. */
    String createPayout(String receiptId, String userId, BigDecimal amount, String currency);
}
