package com.ipld11stars.backend.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default sender for local development: writes the email to the log instead of sending it.
 */
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("[email disabled] to={} subject=\"{}\"", to, subject);
    }
}
