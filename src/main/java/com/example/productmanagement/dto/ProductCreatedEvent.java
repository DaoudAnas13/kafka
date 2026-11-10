package com.example.productmanagement.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductCreatedEvent(
        String eventId,
        String eventType,
        Long productId,
        String name,
        BigDecimal price,
        Integer quantity,
        LocalDateTime occurredAt
) {}