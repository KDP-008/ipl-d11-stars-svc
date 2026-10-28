package com.ipld11stars.backend.wallet;

import com.ipld11stars.backend.BaseIntegrationTest;
import com.ipld11stars.backend.model.PaymentOrder;
import com.ipld11stars.backend.model.User;
import com.ipld11stars.backend.payment.PaymentService;
import com.ipld11stars.backend.payment.WebhookSignatureVerifier;
import com.ipld11stars.backend.repository.jdbc.PaymentOrderRepository;
import com.ipld11stars.backend.service.WalletService;
import com.ipld11stars.backend.support.RecordingEmailSender;
import com.ipld11stars.backend.support.UserFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentWebhookIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private PaymentOrderRepository orderRepository;
    @Autowired
    private WebhookSignatureVerifier verifier;
    @Autowired
    private UserFixtures users;
    @Autowired
    private RecordingEmailSender emails;

    private ResultActions postWebhook(String type, String orderId, String amount) throws Exception {
        return postWebhook("evt_" + UUID.randomUUID(), type, orderId, amount);
    }

    private ResultActions postWebhook(String eventId, String type, String orderId, String amount) throws Exception {
        String body = "{\"id\":\"" + eventId + "\",\"type\":\"" + type + "\",\"data\":{\"orderId\":\"" + orderId
                + "\",\"referenceId\":\"pay_123\",\"amount\":\"" + amount + "\"}}";
        return mockMvc.perform(post("/payments/webhook")
                .header("X-Gateway-Signature", verifier.sign(body))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void depositIsCreditedOnceOnCapturedWebhook() throws Exception {
        User u = users.create("Kiran");
        PaymentOrder order = paymentService.createDeposit(u.getId(), new BigDecimal("500.00"));
        assertThat(order.status()).isEqualTo("CREATED");
        assertThat(walletService.getBalance(u.getId())).isEqualByComparingTo("0");

        String eventId = "evt_" + UUID.randomUUID();
        postWebhook(eventId, "payment.captured", order.gatewayOrderId(), "500.00")
                .andExpect(status().isOk()).andExpect(jsonPath("$.outcome").value("APPLIED"));
        postWebhook(eventId, "payment.captured", order.gatewayOrderId(), "500.00")
                .andExpect(jsonPath("$.outcome").value("DUPLICATE"));
        // a different event id for the same capture (gateway retry with a new id) is also harmless
        postWebhook("payment.captured", order.gatewayOrderId(), "500.00")
                .andExpect(jsonPath("$.outcome").value("ALREADY_APPLIED"));

        assertThat(walletService.getBalance(u.getId())).isEqualByComparingTo("500.00");
        assertThat(orderRepository.findById(order.id()).orElseThrow().status()).isEqualTo("CAPTURED");
        assertThat(emails.sentTo(u.getEmail())).anyMatch(m -> m.subject().contains("500.00"));
    }

    @Test
    void badSignatureAmountMismatchAndStaleEventsAreRejectedOrIgnored() throws Exception {
        User u = users.create("Dev");
        PaymentOrder order = paymentService.createDeposit(u.getId(), new BigDecimal("200.00"));
        String body = "{\"id\":\"evt_x\",\"type\":\"payment.captured\",\"data\":{\"orderId\":\"" + order.gatewayOrderId()
                + "\",\"amount\":\"200.00\"}}";
        mockMvc.perform(post("/payments/webhook").header("X-Gateway-Signature", "deadbeef")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());

        postWebhook("payment.captured", order.gatewayOrderId(), "20.00")
                .andExpect(jsonPath("$.outcome").value("AMOUNT_MISMATCH"));
        assertThat(walletService.getBalance(u.getId())).isEqualByComparingTo("0");

        // failed then captured (customer retried): the capture still credits the wallet
        postWebhook("payment.failed", order.gatewayOrderId(), "200.00").andExpect(jsonPath("$.outcome").value("APPLIED"));
        postWebhook("payment.captured", order.gatewayOrderId(), "200.00").andExpect(jsonPath("$.outcome").value("APPLIED"));
        // a late failure after the capture is ignored
        postWebhook("payment.failed", order.gatewayOrderId(), "200.00").andExpect(jsonPath("$.outcome").value("IGNORED_STALE"));
        assertThat(walletService.getBalance(u.getId())).isEqualByComparingTo("200.00");
    }

    @Test
    void withdrawalHoldsFundsAndReversalReturnsThem() throws Exception {
        User u = users.create("Neha");
        PaymentOrder deposit = paymentService.createDeposit(u.getId(), new BigDecimal("1000.00"));
        postWebhook("payment.captured", deposit.gatewayOrderId(), "1000.00");

        String token = obtainAccessToken(u.getUserId(), UserFixtures.PASSWORD);
        mockMvc.perform(post("/wallet/withdrawals").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\": 50}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/wallet/withdrawals").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\": 1500}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/wallet/withdrawals").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\": 600}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/wallet").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.balance", closeTo(400.0, 0.001)));

        PaymentOrder withdrawal = paymentService.getOrders(u.getId()).stream()
                .filter(o -> o.direction().equals("WITHDRAWAL")).findFirst().orElseThrow();
        postWebhook("payout.processed", withdrawal.gatewayOrderId(), "600.00").andExpect(jsonPath("$.outcome").value("APPLIED"));
        // bank returns the money after the payout was reported as processed
        postWebhook("payout.reversed", withdrawal.gatewayOrderId(), "600.00").andExpect(jsonPath("$.outcome").value("APPLIED"));
        postWebhook("payout.reversed", withdrawal.gatewayOrderId(), "600.00").andExpect(jsonPath("$.outcome").value("ALREADY_APPLIED"));
        // a late "processed" after the reversal does not flip the order back
        postWebhook("payout.processed", withdrawal.gatewayOrderId(), "600.00").andExpect(jsonPath("$.outcome").value("IGNORED_STALE"));

        assertThat(walletService.getBalance(u.getId())).isEqualByComparingTo("1000.00");
        assertThat(orderRepository.findById(withdrawal.id()).orElseThrow().status()).isEqualTo("REVERSED");
    }

    @Test
    void walletEndpointsNeedAuthentication() throws Exception {
        mockMvc.perform(get("/wallet")).andExpect(status().is4xxClientError());
    }
}
