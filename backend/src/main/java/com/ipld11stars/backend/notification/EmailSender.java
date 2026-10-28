package com.ipld11stars.backend.notification;

/**
 * Delivers an email. Implementations should throw on failure; callers record the outcome.
 */
public interface EmailSender {

    void send(String to, String subject, String body);
}
