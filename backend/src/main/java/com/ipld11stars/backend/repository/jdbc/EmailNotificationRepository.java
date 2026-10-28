package com.ipld11stars.backend.repository.jdbc;

import com.ipld11stars.backend.model.EmailNotification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class EmailNotificationRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<EmailNotification> rowMapper = (rs, rowNum) -> {
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp sent = rs.getTimestamp("sent_at");
        return new EmailNotification(rs.getString("id"), rs.getString("user_id"), rs.getString("recipient"),
                rs.getString("template"), rs.getString("subject"), rs.getString("body"), rs.getString("status"),
                rs.getString("error"), created != null ? created.toLocalDateTime() : null,
                sent != null ? sent.toLocalDateTime() : null);
    };

    public EmailNotificationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(EmailNotification n) {
        jdbcTemplate.update("INSERT INTO ipld11stars.email_notifications (id, user_id, recipient, template, subject, body, " +
                        "status, error, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                n.id(), n.userId(), n.recipient(), n.template(), n.subject(), n.body(), n.status(), n.error());
    }

    public void markSent(String id) {
        jdbcTemplate.update("UPDATE ipld11stars.email_notifications SET status = 'SENT', sent_at = NOW(), error = NULL WHERE id = ?", id);
    }

    public void markFailed(String id, String error) {
        String trimmed = error != null && error.length() > 500 ? error.substring(0, 500) : error;
        jdbcTemplate.update("UPDATE ipld11stars.email_notifications SET status = 'FAILED', error = ? WHERE id = ?", trimmed, id);
    }

    public List<EmailNotification> findByUserId(String userId) {
        return jdbcTemplate.query("SELECT * FROM ipld11stars.email_notifications WHERE user_id = ? ORDER BY created_at DESC, id",
                rowMapper, userId);
    }
}
