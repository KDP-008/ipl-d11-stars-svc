package com.ipld11stars.backend.controller;

import com.ipld11stars.backend.model.PaymentOrder;
import com.ipld11stars.backend.model.WalletTransaction;
import com.ipld11stars.backend.payment.PaymentService;
import com.ipld11stars.backend.security.CurrentUser;
import com.ipld11stars.backend.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    private final WalletService walletService;
    private final PaymentService paymentService;

    public WalletController(WalletService walletService, PaymentService paymentService) {
        this.walletService = walletService;
        this.paymentService = paymentService;
    }

    @GetMapping
    public Map<String, Object> balance() {
        return Map.of("balance", walletService.getBalance(CurrentUser.requireId()), "currency", "INR");
    }

    @GetMapping("/transactions")
    public List<WalletTransaction> transactions() {
        return walletService.getTransactions(CurrentUser.requireId());
    }

    @GetMapping("/payments")
    public List<PaymentOrder> payments() {
        return paymentService.getOrders(CurrentUser.requireId());
    }

    @PostMapping("/deposits")
    public ResponseEntity<PaymentOrder> deposit(@RequestBody AmountRequest request) {
        return ResponseEntity.status(201).body(paymentService.createDeposit(CurrentUser.requireId(), request.amount()));
    }

    @PostMapping("/withdrawals")
    public ResponseEntity<PaymentOrder> withdraw(@RequestBody AmountRequest request) {
        return ResponseEntity.status(201).body(paymentService.requestWithdrawal(CurrentUser.requireId(), request.amount()));
    }

    public record AmountRequest(BigDecimal amount) {
    }
}
