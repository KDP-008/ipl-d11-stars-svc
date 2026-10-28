package com.ipld11stars.backend.model;

import java.time.LocalDateTime;

/**
 * An email we tried (or decided not) to send. Status is PENDING, SENT, FAILED or SKIPPED (no address).
 */
public record EmailNotification(String id, String userId, String recipient, String template, String subject,
                                String body, String status, String error, LocalDateTime createdAt,
                                LocalDateTime sentAt) {
}
