package com.rest.controller;

import com.rest.dtos.OrderRequest;
import com.rest.engine.SecureOrderPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {
    private final SecureOrderPublisher publisher;
    public OrderController(SecureOrderPublisher publisher) {
        this.publisher = publisher;
    }
    @PostMapping
    public ResponseEntity<Map<String, Object>> placeOrder(@RequestBody OrderRequest request) {
        String orderId = request.getOrderId();
        if (orderId == null || orderId.isBlank()) {
            orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        publisher.publishOrder(orderId, request.getSide(), request.getPrice(), request.getQuantity());
        return ResponseEntity.accepted().body(Map.of(
                "status", "ACCEPTED",
                "orderId", orderId,
                "message", "Order successfully accepted into Disruptor RingBuffer"
        ));
    }
}

