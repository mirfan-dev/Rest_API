package com.rest.engine;




import com.lmax.disruptor.EventHandler;
import com.rest.dtos.OrderEvent;
import com.rest.dtos.OrderRequest;
import com.rest.dtos.TradeEvent;
import com.rest.enums.OrderSide;
import com.rest.service.impl.KafkaOrderProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;
@Component
public class OrderEventHandler implements EventHandler<OrderEvent> {
    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;
    @Autowired(required = false)
    private KafkaOrderProducer kafkaOrderProducer;
    // BUY → highest price first, then FIFO by sequence (Price-Time Priority)
    private final PriorityQueue<OrderRequest> buyOrders = new PriorityQueue<>(
            Comparator.comparingDouble(OrderRequest::getPrice).reversed()
                    .thenComparingLong(OrderRequest::getSequence)
    );
// SELL → lowest price first, then FIFO by sequence (Price-Time Priority)

    private final PriorityQueue<OrderRequest> sellOrders = new PriorityQueue<>(
            Comparator.comparingDouble(OrderRequest::getPrice)
                    .thenComparingLong(OrderRequest::getSequence)
    );
    // Lock-free read snapshots for HTTP endpoints and WebSocket consumers
    private volatile List<OrderRequest> topBidsSnapshot = Collections.emptyList();
    private volatile List<OrderRequest> topAsksSnapshot = Collections.emptyList();
    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        if (event.getOrderId() == null || event.getSide() == null || event.getQuantity() <= 0) {
            return;
        }
        // Copy Disruptor slot into internal OrderRecord with arrival sequence
        OrderRequest order = new OrderRequest(
                event.getOrderId(),
                event.getSide(),
                event.getPrice(),
                event.getQuantity(),
                sequence,
                System.currentTimeMillis()
        );
        System.out.printf("[OrderFlow Engine] Received %s Order: %s @ $%.2f | Qty: %d (seq=%d)%n",
                order.getSide(), order.getOrderId(), order.getPrice(), order.getQuantity(), sequence);
        // Add to matching queue
        if (order.getSide() == OrderSide.BUY) {
            buyOrders.add(order);
        } else if (order.getSide() == OrderSide.SELL) {
            sellOrders.add(order);
        }
        // Execute matching engine logic
        matchOrders();
        // Update lock-free snapshots for Level 2 depth
        updateSnapshots();

        // Broadcast live Level 2 depth update to WebSocket
        if (messagingTemplate != null) {
            Map<String, Object> payload = Map.of(
                    "bids", topBidsSnapshot,
                    "asks", topAsksSnapshot,
                    "timestamp", System.currentTimeMillis()
            );
            messagingTemplate.convertAndSend("/topic/orderbook", payload);
        }
    }
    private void matchOrders() {
        while (!buyOrders.isEmpty() && !sellOrders.isEmpty()) {
            OrderRequest buy = buyOrders.peek();
            OrderRequest sell = sellOrders.peek();
            // BUY price must be >= SELL price for a match
            if (buy.getPrice() < sell.getPrice()) {
                break;
            }
            int tradedQuantity = Math.min(buy.getQuantity(), sell.getQuantity());
            // Execution price: Resting order price (SELL order price in standard exchange books)
            double tradePrice = sell.getPrice();
            TradeEvent trade = new TradeEvent(
                    buy.getOrderId(),
                    sell.getOrderId(),
                    tradedQuantity,
                    tradePrice
            );
            System.out.printf("[OrderFlow Match] TRADE EXECUTED: %s ↔ %s | Qty: %d | Price: $%.2f%n",
                    buy.getOrderId(), sell.getOrderId(), tradedQuantity, tradePrice);
            // 1. Broadcast trade execution via WebSocket to UI
            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend("/topic/trades", trade);
            }
            // 2. Broadcast trade execution to Kafka (Risk / Clearing microservices)
            if (kafkaOrderProducer != null) {
                kafkaOrderProducer.publishTrade(
                        buy.getOrderId(),
                        sell.getOrderId(),
                        tradedQuantity,
                        tradePrice
                );
            }
            // Adjust remaining quantities
            buy.setQuantity(buy.getQuantity() - tradedQuantity);
            sell.setQuantity(sell.getQuantity() - tradedQuantity);
            if (buy.getQuantity() == 0) {
                buyOrders.poll();
            }
            if (sell.getQuantity() == 0) {
                sellOrders.poll();
            }
        }
    }
    private void updateSnapshots() {
        topBidsSnapshot = buyOrders.stream()
                .sorted(Comparator.comparingDouble(OrderRequest::getPrice).reversed()
                        .thenComparingLong(OrderRequest::getSequence))
                .limit(25)
                .collect(Collectors.toUnmodifiableList());
        topAsksSnapshot = sellOrders.stream()
                .sorted(Comparator.comparingDouble(OrderRequest::getPrice)
                        .thenComparingLong(OrderRequest::getSequence))
                .limit(25)
                .collect(Collectors.toUnmodifiableList());
    }
    public List<OrderRequest> getTopBids() {
        return topBidsSnapshot;
    }
    public List<OrderRequest> getTopAsks() {
        return topAsksSnapshot;
    }
    public synchronized void reset() {
        buyOrders.clear();
        sellOrders.clear();
        topBidsSnapshot = Collections.emptyList();
        topAsksSnapshot = Collections.emptyList();
    }
}


