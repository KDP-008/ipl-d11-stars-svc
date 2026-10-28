package com.ipld11stars.backend.notification;

import com.ipld11stars.backend.model.EmailNotification;
import com.ipld11stars.backend.model.User;
import com.ipld11stars.backend.repository.jdbc.EmailNotificationRepository;
import com.ipld11stars.backend.repository.jdbc.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Queues user emails. The notification row is written in the caller's transaction and the email is
 * only sent once that transaction commits, so a rolled-back action never emails anyone.
 * Delivery failures are recorded on the row and never break the business operation.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final UserRepository userRepository;
    private final EmailNotificationRepository repository;
    private final EmailSender emailSender;

    public NotificationService(UserRepository userRepository, EmailNotificationRepository repository,
                               EmailSender emailSender) {
        this.userRepository = userRepository;
        this.repository = repository;
        this.emailSender = emailSender;
    }

    public void notifyUser(String userId, EmailTemplate template, Map<String, ?> params) {
        User user = userRepository.findById(userId).orElse(null);
        Map<String, Object> values = new HashMap<>(params);
        values.putIfAbsent("firstName", user != null && user.getFirstName() != null ? user.getFirstName() : "there");
        String recipient = user != null && user.getEmail() != null && !user.getEmail().isBlank() ? user.getEmail() : null;

        EmailNotification n = new EmailNotification(UUID.randomUUID().toString(), userId, recipient, template.name(),
                template.renderSubject(values), template.renderBody(values),
                recipient == null ? "SKIPPED" : "PENDING", recipient == null ? "No email address on file" : null,
                null, null);
        repository.insert(n);
        if (recipient == null) {
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deliver(n);
                }
            });
        } else {
            deliver(n);
        }
    }

    public List<EmailNotification> getForUser(String userId) {
        return repository.findByUserId(userId);
    }

    private void deliver(EmailNotification n) {
        try {
            emailSender.send(n.recipient(), n.subject(), n.body());
            repository.markSent(n.id());
        } catch (RuntimeException e) {
            log.warn("Email {} to {} failed: {}", n.template(), n.recipient(), e.getMessage());
            repository.markFailed(n.id(), e.getMessage());
        }
    }
}
