package com.example.productmanagement.service;

import com.example.productmanagement.dto.ProductCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class ProductEventProducer {

    private static final String TOPIC = "product-events";
    private static final Logger log = LoggerFactory.getLogger(ProductEventProducer.class);

    private final KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;

    public ProductEventProducer(KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishProductCreated(ProductCreatedEvent event) {
        String key = String.valueOf(event.productId());

        CompletableFuture<SendResult<String, ProductCreatedEvent>> future =
                kafkaTemplate.send(TOPIC, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Published event [{}] to topic {} partition {} offset {}",
                        event.eventId(), TOPIC,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish event [{}]: {}", event.eventId(), ex.getMessage());
            }
        });
    }

    public static ProductCreatedEvent from(Long productId, String name,
                                           java.math.BigDecimal price, Integer quantity) {
        return new ProductCreatedEvent(
                UUID.randomUUID().toString(),
                "PRODUCT_CREATED",
                productId,
                name,
                price,
                quantity,
                java.time.LocalDateTime.now()
        );
    }
}