package com.example.productmanagement.service;

import com.example.productmanagement.dto.ProductCreatedEvent;
import com.example.productmanagement.dto.ProductRequest;
import com.example.productmanagement.dto.ProductResponse;
import com.example.productmanagement.entity.OutboxEvent;
import com.example.productmanagement.entity.Product;
import com.example.productmanagement.exception.ProductNotFoundException;
import com.example.productmanagement.repository.OutboxEventRepository;
import com.example.productmanagement.repository.ProductRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private static final String TOPIC = "product-events";
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public ProductService(ProductRepository productRepository,
                          OutboxEventRepository outboxEventRepository,
                          ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        // ① Business state
        Product product = new Product(
                request.name(),
                request.description(),
                request.price(),
                request.quantity()
        );
        Product saved = productRepository.save(product);

        // ② Event — SAME transaction as ①. Atomicity lives here.
        ProductCreatedEvent event = new ProductCreatedEvent(
                UUID.randomUUID().toString(),
                "PRODUCT_CREATED",
                saved.getId(),
                saved.getName(),
                saved.getPrice(),
                saved.getQuantity(),
                LocalDateTime.now()
        );
        outboxEventRepository.save(toOutbox(event, saved.getId()));

        return toResponse(saved);
    }

    private OutboxEvent toOutbox(ProductCreatedEvent event, Long productId) {
        try {
            return new OutboxEvent(TOPIC, String.valueOf(productId),
                    objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            // Roll back the whole transaction — never save a product without its event.
            throw new IllegalStateException("Failed to serialize event for product " + productId, e);
        }
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return toResponse(productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id)));
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setQuantity(request.quantity());
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        productRepository.delete(product);
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(), product.getName(), product.getDescription(),
                product.getPrice(), product.getQuantity(),
                product.getCreatedAt(), product.getUpdatedAt());
    }
}