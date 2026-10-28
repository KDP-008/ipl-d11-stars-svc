package com.ipld11stars.backend.wallet;

import com.ipld11stars.backend.BaseIntegrationTest;
import com.ipld11stars.backend.exception.BusinessRuleException;
import com.ipld11stars.backend.model.TransactionType;
import com.ipld11stars.backend.model.User;
import com.ipld11stars.backend.model.WalletTransaction;
import com.ipld11stars.backend.service.WalletService;
import com.ipld11stars.backend.support.UserFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WalletServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WalletService walletService;
    @Autowired
    private UserFixtures users;

    @Test
    void creditsAndDebitsAreLedgeredWithRunningBalance() {
        User u = users.create("Asha");
        assertThat(walletService.getBalance(u.getId())).isEqualByComparingTo("0");

        walletService.credit(u.getId(), new BigDecimal("250.50"), TransactionType.DEPOSIT, "TEST", "1", "k1-" + u.getId(), null);
        WalletTransaction debit = walletService.debit(u.getId(), new BigDecimal("49.50"), TransactionType.ENTRY_FEE,
                "TEST", "2", "k2-" + u.getId(), null);

        assertThat(debit.amount()).isEqualByComparingTo("-49.50");
        assertThat(debit.balanceAfter()).isEqualByComparingTo("201.00");
        assertThat(walletService.getBalance(u.getId())).isEqualByComparingTo("201.00");
        assertThat(walletService.getTransactions(u.getId())).hasSize(2);
    }

    @Test
    void sameIdempotencyKeyMovesMoneyOnce() {
        User u = users.create("Ravi");
        String key = "deposit-replay-" + u.getId();
        WalletTransaction first = walletService.credit(u.getId(), new BigDecimal("100"), TransactionType.DEPOSIT, "T", "1", key, null);
        WalletTransaction again = walletService.credit(u.getId(), new BigDecimal("100"), TransactionType.DEPOSIT, "T", "1", key, null);
        assertThat(again.id()).isEqualTo(first.id());
        assertThat(walletService.getBalance(u.getId())).isEqualByComparingTo("100.00");

        assertThatThrownBy(() -> walletService.credit(u.getId(), new BigDecimal("5"), TransactionType.DEPOSIT, "T", "1", key, null))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("different movement");
    }

    @Test
    void balanceCannotGoNegativeAndAmountsAreValidated() {
        User u = users.create("Meena");
        walletService.credit(u.getId(), new BigDecimal("10"), TransactionType.DEPOSIT, "T", "1", "neg-1-" + u.getId(), null);
        assertThatThrownBy(() -> walletService.debit(u.getId(), new BigDecimal("10.01"), TransactionType.ENTRY_FEE, "T", "2",
                "neg-2-" + u.getId(), null)).hasMessageContaining("Insufficient");
        assertThat(walletService.getBalance(u.getId())).isEqualByComparingTo("10.00");
        assertThatThrownBy(() -> walletService.credit(u.getId(), new BigDecimal("1.005"), TransactionType.DEPOSIT, "T", "3",
                "neg-3-" + u.getId(), null)).hasMessageContaining("two decimal places");
        assertThatThrownBy(() -> walletService.credit(u.getId(), BigDecimal.ZERO, TransactionType.DEPOSIT, "T", "4",
                "neg-4-" + u.getId(), null)).hasMessageContaining("greater than zero");
    }
}
