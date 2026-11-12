package com.example.productmanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

    private final OutboxPublisher outboxPublisher;

    public OutboxPoller(OutboxPublisher outboxPublisher) {
        this.outboxPublisher = outboxPublisher;
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void sweep() {
        try {
            outboxPublisher.publishPending();
        } catch (Exception ex) {
            // Never let a sweep crash the scheduler thread.
            log.error("Outbox sweep failed: {}", ex.getMessage());
        }
    }
}