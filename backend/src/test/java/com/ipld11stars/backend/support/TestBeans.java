package com.ipld11stars.backend.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Beans shared by every integration test (imported by BaseIntegrationTest so the context is reused).
 */
@TestConfiguration
public class TestBeans {

    @Bean
    @Primary
    public RecordingEmailSender recordingEmailSender() {
        return new RecordingEmailSender();
    }
}
