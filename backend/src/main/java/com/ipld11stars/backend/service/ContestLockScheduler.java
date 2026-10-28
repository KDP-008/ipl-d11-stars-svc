package com.ipld11stars.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Closes contests at match start even if no delivery has been recorded yet.
 */
@Component
@ConditionalOnProperty(name = "contests.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class ContestLockScheduler {

    private static final Logger log = LoggerFactory.getLogger(ContestLockScheduler.class);

    private final ContestLifecycleService lifecycleService;

    public ContestLockScheduler(ContestLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    @Scheduled(fixedDelayString = "${contests.lock-check-interval-ms:60000}")
    public void lockDueContests() {
        int handled = lifecycleService.lockDueContests();
        if (handled > 0) {
            log.info("Closed {} contests whose match has started", handled);
        }
    }
}
