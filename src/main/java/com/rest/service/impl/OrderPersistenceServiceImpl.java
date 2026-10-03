package com.rest.service.impl;

import com.rest.entity.Customer;
import com.rest.entity.Trade;
import com.rest.entity.TradeOrder;
import com.rest.enums.OrderSide;
import com.rest.enums.OrderStatus;
import com.rest.enums.OrderType;
import com.rest.repository.CustomerRepository;
import com.rest.repository.OrderRepository;
import com.rest.repository.TradeRepository;
import com.rest.service.OrderPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPersistenceServiceImpl implements OrderPersistenceService {
    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final CustomerRepository customerRepository;
    // Dedicated single-writer thread for asynchronous database persistence
    // Ensures LMAX Disruptor matching thread is never blocked by DB disk I/O
    private final ExecutorService dbPersistenceExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "OrderFlow-DB-Writer");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    @Transactional
    public TradeOrder createAndSaveOrder(String orderId, OrderSide side, OrderType orderType, double price, int quantity, Customer customer) {
        TradeOrder order = TradeOrder.builder()
                .orderId(orderId)
                .side(side)
                .orderType(orderType != null ? orderType : OrderType.LIMIT)
                .price(price)
                .initialQuantity(quantity)
                .remainingQuantity(quantity)
                .status(OrderStatus.PENDING)
                .customer(customer)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return orderRepository.save(order);
    }
    @Override
    public void recordTradeExecution(String buyOrderId, String sellOrderId, int quantity, double price, long executionLatencyMicros) {
        // Enqueue async persistence task so matching engine maintains microsecond latency
        dbPersistenceExecutor.submit(() -> {
            try {
                processTradeInDatabase(buyOrderId, sellOrderId, quantity, price, executionLatencyMicros);
            } catch (Exception e) {
                log.error("[OrderFlow DB] Failed to record trade execution: {}", e.getMessage(), e);
            }
        });
    }

    @Transactional
    protected void processTradeInDatabase(String buyOrderId, String sellOrderId, int quantity, double price, long executionLatencyMicros) {
        Optional<TradeOrder> buyOpt = orderRepository.findByOrderId(buyOrderId);
        Optional<TradeOrder> sellOpt = orderRepository.findByOrderId(sellOrderId);
        Long buyerId = null;
        if (buyOpt.isPresent()) {
            TradeOrder buyOrder = buyOpt.get();
            if (buyOrder.getCustomer() != null) {
                buyerId = buyOrder.getCustomer().getId();
            }
            int newRemaining = Math.max(0, buyOrder.getRemainingQuantity() - quantity);
            buyOrder.setRemainingQuantity(newRemaining);
            buyOrder.setStatus(newRemaining == 0 ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED);
            buyOrder.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(buyOrder);
        }
        Long sellerId = null;
        if (sellOpt.isPresent()) {
            TradeOrder sellOrder = sellOpt.get();
            if (sellOrder.getCustomer() != null) {
                sellerId = sellOrder.getCustomer().getId();
            }
            int newRemaining = Math.max(0, sellOrder.getRemainingQuantity() - quantity);
            sellOrder.setRemainingQuantity(newRemaining);
            sellOrder.setStatus(newRemaining == 0 ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED);
            sellOrder.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(sellOrder);
        }

        Trade trade = Trade.builder()
                .tradeId("TRD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .buyOrderId(buyOrderId)
                .sellOrderId(sellOrderId)
                .buyerCustomerId(buyerId)
                .sellerCustomerId(sellerId)
                .price(price)
                .quantity(quantity)
                .totalAmount(price * quantity)
                .executionLatencyMicros(executionLatencyMicros)
                .executedAt(LocalDateTime.now())
                .build();
        tradeRepository.save(trade);
        log.info("[OrderFlow DB] Saved Trade: {} | {} ↔ {} | Qty: {} @ ${}",
                trade.getTradeId(), buyOrderId, sellOrderId, quantity, price);
    }

    @Override
    @Transactional
    public boolean cancelOrder(String orderId) {
        Optional<TradeOrder> opt = orderRepository.findByOrderId(orderId);
        if (opt.isPresent()) {
            TradeOrder order = opt.get();
            if (order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.PARTIALLY_FILLED) {
                order.setStatus(OrderStatus.CANCELLED);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
                return true;
            }
        }
        return false;
    }

    @Override
    public Optional<TradeOrder> getOrderByOrderId(String orderId) {
        return orderRepository.findByOrderId(orderId);
    }
    @Override
    public List<TradeOrder> getOrdersByCustomer(Customer customer) {
        return orderRepository.findByCustomerOrderByCreatedAtDesc(customer);
    }
    @Override
    public List<TradeOrder> getRecentOrders() {
        return orderRepository.findTop50ByOrderByCreatedAtDesc();
    }
    @Override
    public List<Trade> getRecentTrades() {
        return tradeRepository.findTop50ByOrderByExecutedAtDesc();
    }
    @Override
    public List<Trade> getTradesByCustomer(Long customerId) {
        return tradeRepository.findByBuyerCustomerIdOrSellerCustomerIdOrderByExecutedAtDesc(customerId, customerId);
    }
}
