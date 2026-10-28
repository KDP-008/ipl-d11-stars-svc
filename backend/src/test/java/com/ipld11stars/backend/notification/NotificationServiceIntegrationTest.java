package com.ipld11stars.backend.notification;

import com.ipld11stars.backend.BaseIntegrationTest;
import com.ipld11stars.backend.model.EmailNotification;
import com.ipld11stars.backend.model.User;
import com.ipld11stars.backend.support.RecordingEmailSender;
import com.ipld11stars.backend.support.UserFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private NotificationService notifications;
    @Autowired
    private RecordingEmailSender emails;
    @Autowired
    private UserFixtures users;
    @Autowired
    private TransactionTemplate tx;

    @Test
    void emailIsSentOnlyAfterTheTransactionCommits() {
        User u = users.create("Priya");
        tx.executeWithoutResult(status -> {
            notifications.notifyUser(u.getId(), EmailTemplate.DEPOSIT_SUCCESS, Map.of("amount", "100.00", "balance", "100.00"));
            assertThat(emails.sentTo(u.getEmail())).as("not sent before commit").isEmpty();
        });
        assertThat(emails.sentTo(u.getEmail())).hasSize(1);
        assertThat(emails.sentTo(u.getEmail()).get(0).body()).contains("Hi Priya").contains("Rs 100.00");
        assertThat(notifications.getForUser(u.getId())).extracting(EmailNotification::status).containsExactly("SENT");
    }

    @Test
    void rolledBackActionsSendNothing() {
        User u = users.create("Arjun");
        tx.executeWithoutResult(status -> {
            notifications.notifyUser(u.getId(), EmailTemplate.DEPOSIT_SUCCESS, Map.of("amount", "1", "balance", "1"));
            status.setRollbackOnly();
        });
        assertThat(emails.sentTo(u.getEmail())).isEmpty();
        assertThat(notifications.getForUser(u.getId())).isEmpty();
    }

    @Test
    void missingAddressIsSkippedAndSmtpFailureIsRecorded() {
        User noEmail = users.create("Vikram", false);
        notifications.notifyUser(noEmail.getId(), EmailTemplate.WITHDRAWAL_PROCESSED, Map.of("amount", "10"));
        assertThat(notifications.getForUser(noEmail.getId())).extracting(EmailNotification::status).containsExactly("SKIPPED");

        User u = users.create("Sara");
        emails.failNextSend();
        notifications.notifyUser(u.getId(), EmailTemplate.WITHDRAWAL_PROCESSED, Map.of("amount", "10"));
        List<EmailNotification> rows = notifications.getForUser(u.getId());
        assertThat(rows).extracting(EmailNotification::status).containsExactly("FAILED");
        assertThat(rows.get(0).error()).contains("SMTP unavailable");
    }
}
