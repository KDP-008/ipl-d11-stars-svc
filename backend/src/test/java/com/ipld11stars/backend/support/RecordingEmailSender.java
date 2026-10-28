package com.ipld11stars.backend.support;

import com.ipld11stars.backend.notification.EmailSender;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test email transport that keeps every message in memory. Set {@code failNext} to simulate an outage.
 */
public class RecordingEmailSender implements EmailSender {

    public record Sent(String to, String subject, String body) {
    }

    private final List<Sent> sent = new CopyOnWriteArrayList<>();
    private volatile boolean failNext;

    @Override
    public void send(String to, String subject, String body) {
        if (failNext) {
            failNext = false;
            throw new IllegalStateException("SMTP unavailable");
        }
        sent.add(new Sent(to, subject, body));
    }

    public List<Sent> sentTo(String to) {
        return sent.stream().filter(s -> s.to().equals(to)).toList();
    }

    public void failNextSend() {
        failNext = true;
    }
}
