package com.ipld11stars.backend.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipld11stars.backend.exception.BusinessRuleException;
import com.ipld11stars.backend.model.PaymentOrder;
import com.ipld11stars.backend.model.TransactionType;
import com.ipld11stars.backend.model.WalletTransaction;
import com.ipld11stars.backend.notification.EmailTemplate;
import com.ipld11stars.backend.notification.NotificationService;
import com.ipld11stars.backend.repository.jdbc.PaymentOrderRepository;
import com.ipld11stars.backend.repository.jdbc.PaymentWebhookEventRepository;
import com.ipld11stars.backend.service.WalletService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.ipld11stars.backend.model.PaymentOrder.*;

/**
 * Wallet top-ups and withdrawals through the payment gateway.
 *
 * Money only moves on verified webhooks for deposits; withdrawals hold the amount in the wallet
 * immediately and give it back if the payout is reversed. Webhooks are de-duplicated by event id
 * and every wallet movement carries an idempotency key, so retries and replays are harmless.
 * Events can arrive out of order; stale ones are recorded and ignored.
 */
@Service
public class PaymentService {

    public static final BigDecimal MIN_DEPOSIT = new BigDecimal("10.00");
    public static final BigDecimal MAX_DEPOSIT = new BigDecimal("50000.00");
    public static final BigDecimal MIN_WITHDRAWAL = new BigDecimal("100.00");
    private static final String CURRENCY = "INR";

    private final PaymentGateway gateway;
    private final WebhookSignatureVerifier signatureVerifier;
    private final PaymentOrderRepository orderRepository;
    private final PaymentWebhookEventRepository eventRepository;
    private final WalletService walletService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public PaymentService(PaymentGateway gateway, WebhookSignatureVerifier signatureVerifier,
                          PaymentOrderRepository orderRepository, PaymentWebhookEventRepository eventRepository,
                          WalletService walletService, NotificationService notificationService,
                          ObjectMapper objectMapper) {
        this.gateway = gateway;
        this.signatureVerifier = signatureVerifier;
        this.orderRepository = orderRepository;
        this.eventRepository = eventRepository;
        this.walletService = walletService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PaymentOrder createDeposit(String userId, BigDecimal amount) {
        checkRange(amount, MIN_DEPOSIT, MAX_DEPOSIT, "Deposit");
        String id = UUID.randomUUID().toString();
        String gatewayOrderId = gateway.createOrder(id, amount, CURRENCY);
        orderRepository.insert(new PaymentOrder(id, userId, DEPOSIT, amount, CURRENCY, CREATED, gatewayOrderId,
                null, null, null, null));
        return orderRepository.findById(id).orElseThrow();
    }

    @Transactional
    public PaymentOrder requestWithdrawal(String userId, BigDecimal amount) {
        checkRange(amount, MIN_WITHDRAWAL, null, "Withdrawal");
        String id = UUID.randomUUID().toString();
        orderRepository.insert(new PaymentOrder(id, userId, WITHDRAWAL, amount, CURRENCY, PROCESSING, null,
                null, null, null, null));
        WalletTransaction hold = walletService.debit(userId, amount, TransactionType.WITHDRAWAL, "PAYMENT_ORDER", id,
                "withdrawal:" + id, "Withdrawal to bank");
        String payoutId = gateway.createPayout(id, userId, amount, CURRENCY);
        orderRepository.updateGatewayOrderId(id, payoutId);
        notificationService.notifyUser(userId, EmailTemplate.WITHDRAWAL_REQUESTED,
                Map.of("amount", amount, "balance", hold.balanceAfter()));
        return orderRepository.findById(id).orElseThrow();
    }

    public List<PaymentOrder> getOrders(String userId) {
        return orderRepository.findByUserId(userId);
    }

    /**
     * Handles a gateway webhook. Returns the outcome recorded for the event (e.g. APPLIED, DUPLICATE).
     * Throws 401 for a bad signature and 400 for an unreadable payload.
     */
    @Transactional
    public String handleWebhook(String rawBody, String signature) {
        if (!signatureVerifier.isValid(rawBody, signature)) {
            throw new BusinessRuleException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
        }
        WebhookEvent event = parse(rawBody);
        String gatewayOrderId = event.data() != null ? event.data().orderId() : null;
        if (!eventRepository.tryInsert(event.id(), event.type(), gatewayOrderId, rawBody)) {
            return "DUPLICATE";
        }
        String outcome = process(event, gatewayOrderId);
        eventRepository.updateOutcome(event.id(), outcome);
        return outcome;
    }

    private String process(WebhookEvent event, String gatewayOrderId) {
        if (gatewayOrderId == null) {
            return "IGNORED_NO_ORDER";
        }
        PaymentOrder order = orderRepository.lockByGatewayOrderId(gatewayOrderId).orElse(null);
        if (order == null) {
            return "IGNORED_UNKNOWN_ORDER";
        }
        return switch (event.type()) {
            case WebhookEvent.PAYMENT_CAPTURED -> onCaptured(order, event.data());
            case WebhookEvent.PAYMENT_FAILED -> onPaymentFailed(order, event.data());
            case WebhookEvent.PAYOUT_PROCESSED -> onPayoutProcessed(order);
            case WebhookEvent.PAYOUT_REVERSED, WebhookEvent.PAYOUT_FAILED -> onPayoutReversed(order, event.data());
            default -> "IGNORED_UNKNOWN_TYPE";
        };
    }

    private String onCaptured(PaymentOrder order, WebhookEvent.Data data) {
        if (!DEPOSIT.equals(order.direction())) {
            return "IGNORED_WRONG_DIRECTION";
        }
        if (CAPTURED.equals(order.status())) {
            return "ALREADY_APPLIED";
        }
        if (data.amount() == null || data.amount().compareTo(order.amount()) != 0) {
            return "AMOUNT_MISMATCH";
        }
        // A capture can follow an earlier failure when the customer retries on the same order.
        WalletTransaction credit = walletService.credit(order.userId(), order.amount(), TransactionType.DEPOSIT,
                "PAYMENT_ORDER", order.id(), "deposit:" + order.id(), "Wallet top-up");
        orderRepository.updateStatus(order.id(), CAPTURED, data.referenceId(), null);
        notificationService.notifyUser(order.userId(), EmailTemplate.DEPOSIT_SUCCESS,
                Map.of("amount", order.amount(), "balance", credit.balanceAfter()));
        return "APPLIED";
    }

    private String onPaymentFailed(PaymentOrder order, WebhookEvent.Data data) {
        if (!DEPOSIT.equals(order.direction())) {
            return "IGNORED_WRONG_DIRECTION";
        }
        if (!CREATED.equals(order.status())) {
            return "IGNORED_STALE";
        }
        orderRepository.updateStatus(order.id(), FAILED, data.referenceId(), data.reason());
        return "APPLIED";
    }

    private String onPayoutProcessed(PaymentOrder order) {
        if (!WITHDRAWAL.equals(order.direction())) {
            return "IGNORED_WRONG_DIRECTION";
        }
        if (PAID_OUT.equals(order.status())) {
            return "ALREADY_APPLIED";
        }
        if (!PROCESSING.equals(order.status())) {
            return "IGNORED_STALE";
        }
        orderRepository.updateStatus(order.id(), PAID_OUT, null, null);
        notificationService.notifyUser(order.userId(), EmailTemplate.WITHDRAWAL_PROCESSED, Map.of("amount", order.amount()));
        return "APPLIED";
    }

    private String onPayoutReversed(PaymentOrder order, WebhookEvent.Data data) {
        if (!WITHDRAWAL.equals(order.direction())) {
            return "IGNORED_WRONG_DIRECTION";
        }
        if (REVERSED.equals(order.status())) {
            return "ALREADY_APPLIED";
        }
        // Banks can return a payout even after it was reported as processed.
        walletService.credit(order.userId(), order.amount(), TransactionType.WITHDRAWAL_REVERSAL, "PAYMENT_ORDER",
                order.id(), "withdrawal-reversal:" + order.id(), "Withdrawal returned");
        orderRepository.updateStatus(order.id(), REVERSED, null, data != null ? data.reason() : null);
        notificationService.notifyUser(order.userId(), EmailTemplate.WITHDRAWAL_REVERSED, Map.of("amount", order.amount()));
        return "APPLIED";
    }

    private WebhookEvent parse(String rawBody) {
        try {
            WebhookEvent event = objectMapper.readValue(rawBody, WebhookEvent.class);
            if (event.id() == null || event.type() == null) {
                throw new BusinessRuleException("Webhook must have an id and a type");
            }
            return event;
        } catch (JsonProcessingException e) {
            throw new BusinessRuleException("Unreadable webhook payload");
        }
    }

    private static void checkRange(BigDecimal amount, BigDecimal min, BigDecimal max, String what) {
        if (amount == null || amount.compareTo(min) < 0) {
            throw new BusinessRuleException(what + " must be at least Rs " + min);
        }
        if (max != null && amount.compareTo(max) > 0) {
            throw new BusinessRuleException(what + " cannot exceed Rs " + max);
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new BusinessRuleException("Amounts can have at most two decimal places");
        }
    }
}
