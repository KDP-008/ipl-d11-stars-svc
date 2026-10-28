package com.ipld11stars.backend.controller;

import com.ipld11stars.backend.payment.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Receives payment gateway webhooks. Public endpoint; authenticity comes from the HMAC signature.
 */
@RestController
@RequestMapping("/payments")
public class PaymentWebhookController {

    private final PaymentService paymentService;

    public PaymentWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/webhook")
    public Map<String, String> webhook(@RequestBody String rawBody,
                                       @RequestHeader(value = "X-Gateway-Signature", required = false) String signature) {
        return Map.of("outcome", paymentService.handleWebhook(rawBody, signature));
    }
}
