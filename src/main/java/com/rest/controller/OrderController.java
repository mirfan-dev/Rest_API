package com.rest.controller;

import com.rest.dtos.OrderRequest;
import com.rest.engine.OrderEventHandler;
import com.rest.engine.SecureOrderPublisher;
import com.rest.entity.Customer;
import com.rest.entity.TradeOrder;
import com.rest.enums.OrderSide;
import com.rest.enums.OrderType;
import com.rest.repository.CustomerRepository;
import com.rest.service.OrderPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final SecureOrderPublisher publisher;
    private final OrderEventHandler orderEventHandler;
    private final OrderPersistenceService orderPersistenceService;
    private final CustomerRepository customerRepository;
    @PostMapping
    public ResponseEntity<Map<String, Object>> placeOrder(@RequestBody OrderRequest request) {
        String orderId = request.getOrderId();
        if (orderId == null || orderId.isBlank()) {
            orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        // Resolve authenticated user from SecurityContext or customerId
        Customer customer = resolveCustomer(request.getCustomerId());
        OrderType orderType = request.getOrderType() != null ? request.getOrderType() : OrderType.LIMIT;
        // 1. Persist the Order in MySQL 'orders' table
        TradeOrder savedOrder = orderPersistenceService.createAndSaveOrder(
                orderId,
                request.getSide(),
                orderType,
                request.getPrice(),
                request.getQuantity(),
                customer
        );
        // 2. Publish order into LMAX Disruptor lock-free RingBuffer for microsecond matching
        publisher.publishOrder(
                orderId,
                request.getSide(),
                orderType,
                request.getPrice(),
                request.getQuantity(),
                customer != null ? customer.getId() : null
        );

        log.info("[OrderFlow API] Order placed & saved: {} | {} {} @ ${} | User: {}",
                orderId, request.getSide(), request.getQuantity(), request.getPrice(),
                customer != null ? customer.getEmail() : "GuestTrader");
        return ResponseEntity.accepted().body(Map.of(
                "status", "ACCEPTED",
                "orderId", orderId,
                "order", savedOrder,
                "message", "Order successfully persisted to DB and published to Disruptor RingBuffer"
        ));
    }
    @GetMapping
    public ResponseEntity<List<TradeOrder>> getAllOrders() {
        return ResponseEntity.ok(orderPersistenceService.getRecentOrders());
    }
    @GetMapping("/my-orders")
    public ResponseEntity<?> getMyOrders() {
        Customer customer = resolveCustomer(null);
        if (customer == null) {
            return ResponseEntity.ok(orderPersistenceService.getRecentOrders());
        }
        return ResponseEntity.ok(orderPersistenceService.getOrdersByCustomer(customer));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderById(@PathVariable String orderId) {
        return orderPersistenceService.getOrderByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable String orderId) {
        boolean cancelled = orderEventHandler.cancelOrder(orderId);
        orderPersistenceService.cancelOrder(orderId);
        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "cancelled", cancelled,
                "status", "CANCELLED",
                "message", cancelled ? "Order cancelled successfully" : "Order was not active in memory or already filled"
        ));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrderPost(@PathVariable String orderId) {
        return cancelOrder(orderId);
    }
    @PostMapping("/flurry")
    public ResponseEntity<Map<String, Object>> submitFlurry(
            @RequestParam(defaultValue = "20") int count,
            @RequestParam(defaultValue = "150.0") double basePrice) {
        Random random = new Random();
        List<String> orderIds = new ArrayList<>();
        Customer customer = resolveCustomer(null);
        long startNanos = System.nanoTime();
        for (int i = 0; i < count; i++) {
            String orderId = "FLURRY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            OrderSide side = random.nextBoolean() ? OrderSide.BUY : OrderSide.SELL;
            double spreadOffset = (random.nextInt(30) - 15) * 0.25;
            double price = Math.round((basePrice + spreadOffset) * 100.0) / 100.0;
            if (price <= 0) price = 10.0;
            int quantity = (random.nextInt(10) + 1) * 10;
            // Save to DB
            orderPersistenceService.createAndSaveOrder(
                    orderId, side, OrderType.LIMIT, price, quantity, customer
            );
            // Publish to Disruptor
            publisher.publishOrder(
                    orderId, side, OrderType.LIMIT, price, quantity,
                    customer != null ? customer.getId() : null
            );
            orderIds.add(orderId);
        }
        long elapsedMicros = (System.nanoTime() - startNanos) / 1000;
        double throughputTps = count / (Math.max(1, elapsedMicros) / 1_000_000.0);
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "count", count,
                "elapsedMicros", elapsedMicros,
                "estimatedTps", Math.round(throughputTps),
                "orderIds", orderIds,
                "message", String.format("Processed flurry of %d orders in %d µs lock-free", count, elapsedMicros)

        ));
    }
    private Customer resolveCustomer(Long requestedCustomerId) {
        if (requestedCustomerId != null) {
            Optional<Customer> opt = customerRepository.findById(requestedCustomerId);
            if (opt.isPresent()) return opt.get();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equalsIgnoreCase("anonymousUser")) {
            return customerRepository.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }
}

