package com.ipld11stars.backend.notification;

import java.util.Map;

/**
 * Email templates. Placeholders look like {name} and are filled from the params map.
 */
public enum EmailTemplate {
    DEPOSIT_SUCCESS(
            "Wallet top-up of Rs {amount} received",
            "Hi {firstName},\n\nWe have added Rs {amount} to your IPL D11 Stars wallet. Your balance is now Rs {balance}.\n"),
    WITHDRAWAL_REQUESTED(
            "Withdrawal of Rs {amount} requested",
            "Hi {firstName},\n\nYour withdrawal of Rs {amount} is being processed. Your balance is now Rs {balance}.\n"),
    WITHDRAWAL_PROCESSED(
            "Withdrawal of Rs {amount} paid out",
            "Hi {firstName},\n\nRs {amount} has been sent to your bank account.\n"),
    WITHDRAWAL_REVERSED(
            "Withdrawal of Rs {amount} returned to your wallet",
            "Hi {firstName},\n\nYour withdrawal of Rs {amount} could not be completed and the money is back in your wallet.\n"),
    ENTRY_CONFIRMED(
            "You're in: {contestName}",
            "Hi {firstName},\n\nYour team {entryName} has joined {contestName}. Entry fee paid: Rs {entryFee}.\n"),
    CONTEST_CANCELLED(
            "{contestName} was cancelled",
            "Hi {firstName},\n\n{contestName} did not go ahead ({reason}). Your entry fee of Rs {entryFee} has been refunded.\n"),
    CONTEST_RESULT(
            "Results for {contestName}",
            "Hi {firstName},\n\nYour team {entryName} finished at rank {rank} with {points} points in {contestName}. Winnings: Rs {prize}.\n");

    private final String subject;
    private final String body;

    EmailTemplate(String subject, String body) {
        this.subject = subject;
        this.body = body;
    }

    public String renderSubject(Map<String, ?> params) {
        return render(subject, params);
    }

    public String renderBody(Map<String, ?> params) {
        return render(body, params);
    }

    static String render(String text, Map<String, ?> params) {
        String out = text;
        for (Map.Entry<String, ?> e : params.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", String.valueOf(e.getValue()));
        }
        return out;
    }
}
