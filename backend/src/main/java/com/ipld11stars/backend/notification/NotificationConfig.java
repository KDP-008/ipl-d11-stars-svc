package com.ipld11stars.backend.notification;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Picks the email transport: SMTP when app.mail.enabled=true (needs spring.mail.host), otherwise log only.
 */
@Configuration
public class NotificationConfig {

    @Bean
    public EmailSender emailSender(@Value("${app.mail.enabled:false}") boolean enabled,
                                   @Value("${app.mail.from:no-reply@ipld11stars.com}") String from,
                                   ObjectProvider<JavaMailSender> mailSender) {
        if (enabled) {
            JavaMailSender sender = mailSender.getIfAvailable();
            if (sender == null) {
                throw new IllegalStateException("app.mail.enabled=true but no mail server is configured (spring.mail.host)");
            }
            return new SmtpEmailSender(sender, from);
        }
        return new LoggingEmailSender();
    }
}
