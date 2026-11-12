package com.example.productmanagement.service;

import com.example.productmanagement.entity.OutboxEvent;
import com.example.productmanagement.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                           KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes pending outbox events. Each event's "published" marking is its
     * own transaction, independent of others — one poison row must not block
     * the rest (per-row isolation).
     */
    public void publishPending() {
        List<OutboxEvent> pending =
                outboxEventRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();

        for (OutboxEvent event : pending) {
            try {
                publishOne(event);
            } catch (Exception ex) {
                // Leave unpublished; the next sweep retries (at-least-once).
                log.error("Failed to publish outbox event id={} (attempt {}): {}",
                        event.getId(), event.getAttempts() + 1, ex.getMessage());
                incrementAttempts(event.getId());
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void publishOne(OutboxEvent event) throws Exception {
        // .get(3s) → synchronous per event: we only mark published AFTER the
        // broker acknowledged. StringSerializer sends payload bytes as-is.
        kafkaTemplate.send(event.getTopic(), event.getMessageKey(), event.getPayload())
                .get(3, TimeUnit.SECONDS);

        event.markPublished();
        outboxEventRepository.save(event);
        log.info("Outbox event id={} published to {} (key={})",
                event.getId(), event.getTopic(), event.getMessageKey());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void incrementAttempts(Long id) {
        outboxEventRepository.findById(id).ifPresent(e -> {
            e.incrementAttempts();
            outboxEventRepository.save(e);
        });
    }
}