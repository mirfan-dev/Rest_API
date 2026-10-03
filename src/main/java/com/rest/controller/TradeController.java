package com.rest.controller;

import com.rest.entity.Customer;
import com.rest.entity.Trade;
import com.rest.repository.CustomerRepository;
import com.rest.service.OrderPersistenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.List;
@RestController
@RequestMapping("/api/trades")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TradeController {
    private final OrderPersistenceService orderPersistenceService;
    private final CustomerRepository customerRepository;
    @GetMapping
    public ResponseEntity<List<Trade>> getAllTrades() {
        return ResponseEntity.ok(orderPersistenceService.getRecentTrades());
    }
    @GetMapping("/recent")
    public ResponseEntity<List<Trade>> getRecentTrades() {
        return ResponseEntity.ok(orderPersistenceService.getRecentTrades());
    }

    @GetMapping("/my-trades")
    public ResponseEntity<List<Trade>> getMyTrades() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equalsIgnoreCase("anonymousUser")) {
            Customer customer = customerRepository.findByEmail(auth.getName()).orElse(null);
            if (customer != null) {
                return ResponseEntity.ok(orderPersistenceService.getTradesByCustomer(customer.getId()));
            }
        }
        return ResponseEntity.ok(orderPersistenceService.getRecentTrades());
    }
}