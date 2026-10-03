package com.rest.engine;




import com.lmax.disruptor.EventHandler;
import com.rest.dtos.OrderEvent;
import com.rest.dtos.OrderRequest;
import com.rest.dtos.TradeEvent;
import com.rest.enums.OrderSide;
import com.rest.enums.OrderType;
import com.rest.service.OrderPersistenceService;
import com.rest.service.impl.KafkaOrderProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
@Slf4j
@Component
public class OrderEventHandler implements EventHandler<OrderEvent> {
    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;
    @Autowired(required = false)
    private KafkaOrderProducer kafkaOrderProducer;
    @Autowired(required = false)
    private OrderPersistenceService orderPersistenceService;
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
    // High throughput performance metrics
    private final AtomicLong totalOrdersProcessed = new AtomicLong(0);
    private final AtomicLong totalTradesExecuted = new AtomicLong(0);
    private volatile long lastMatchLatencyMicros = 18; // default baseline microsecond latency
    private volatile double totalTradingVolume = 0.0;
    private volatile double lastTradedPrice = 150.00;
    // SSE listeners for reactive market data gateway
    private final List<Consumer<Map<String, Object>>> sseOrderBookListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<TradeEvent>> sseTradeListeners = new CopyOnWriteArrayList<>();

    public void addOrderBookListener(Consumer<Map<String, Object>> listener) {
        sseOrderBookListeners.add(listener);
    }
    public void removeOrderBookListener(Consumer<Map<String, Object>> listener) {
        sseOrderBookListeners.remove(listener);
    }
    public void addTradeListener(Consumer<TradeEvent> listener) {
        sseTradeListeners.add(listener);
    }
    public void removeTradeListener(Consumer<TradeEvent> listener) {
        sseTradeListeners.remove(listener);
    }
    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        long startNanos = System.nanoTime();
        if (event.getOrderId() == null || event.getSide() == null || event.getQuantity() <= 0) {
            return;
        }
        totalOrdersProcessed.incrementAndGet();

        // Copy Disruptor slot into internal OrderRequest record with arrival sequence
        OrderRequest order = new OrderRequest(
                event.getOrderId(),
                event.getSide(),
                event.getOrderType() != null ? event.getOrderType() : OrderType.LIMIT,
                event.getPrice(),
                event.getQuantity(),
                sequence,
                System.currentTimeMillis(),
                event.getCustomerId()
        );
        log.debug("[OrderFlow Engine] Received {} Order: {} @ ${} | Qty: {} (seq={})",
                order.getSide(), order.getOrderId(), order.getPrice(), order.getQuantity(), sequence);
        // Add to matching queue
        if (order.getSide() == OrderSide.BUY) {
            buyOrders.add(order);
        } else if (order.getSide() == OrderSide.SELL) {
            sellOrders.add(order);
        }
        // Execute matching engine logic (Price-Time Priority)
        matchOrders(startNanos);
        // Update lock-free snapshots for Level 2 depth
        updateSnapshots();

        // Broadcast live Level 2 depth update to WebSocket
        Map<String, Object> payload = Map.of(
                "bids", topBidsSnapshot,
                "asks", topAsksSnapshot,
                "timestamp", System.currentTimeMillis()
        );
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend("/topic/orderbook", payload);
        }
        // Broadcast to SSE listeners
        for (Consumer<Map<String, Object>> listener : sseOrderBookListeners) {
            try {
                listener.accept(payload);
            } catch (Exception ignored) {}
        }
    }
    private void matchOrders(long startNanos) {
        while (!buyOrders.isEmpty() && !sellOrders.isEmpty()) {
            OrderRequest buy = buyOrders.peek();
            OrderRequest sell = sellOrders.peek();
            // Check price crossing: BUY price must be >= SELL price
            if (buy.getPrice() < sell.getPrice()) {
                break;
            }

            int tradedQuantity = Math.min(buy.getQuantity(), sell.getQuantity());
            // Execution price: Resting order price (SELL order price in standard exchange books)
            double tradePrice = sell.getPrice();
            lastTradedPrice = tradePrice;
            totalTradingVolume += tradePrice * tradedQuantity;
            totalTradesExecuted.incrementAndGet();
            long latencyMicros = Math.max(1, (System.nanoTime() - startNanos) / 1000L);
            lastMatchLatencyMicros = latencyMicros;
            TradeEvent trade = new TradeEvent(
                    buy.getOrderId(),
                    sell.getOrderId(),
                    tradedQuantity,
                    tradePrice
            );
            log.info("[OrderFlow Match] TRADE EXECUTED in {}µs: {} ↔ {} | Qty: {} | Price: ${}",
                    latencyMicros, buy.getOrderId(), sell.getOrderId(), tradedQuantity, tradePrice);
            // 1. Broadcast trade execution via WebSocket to UI
            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend("/topic/trades", trade);
            }

            // 2. Broadcast to SSE listeners
            for (Consumer<TradeEvent> listener : sseTradeListeners) {
                try {
                    listener.accept(trade);
                } catch (Exception ignored) {}
            }
            // 3. Broadcast trade execution to Kafka (Risk / Clearing microservices)
            if (kafkaOrderProducer != null) {
                kafkaOrderProducer.publishTrade(
                        buy.getOrderId(),
                        sell.getOrderId(),
                        tradedQuantity,
                        tradePrice
                );
            }
            // 4. Asynchronously persist trade execution & update order statuses in MySQL database
            if (orderPersistenceService != null) {
                orderPersistenceService.recordTradeExecution(
                        buy.getOrderId(),
                        sell.getOrderId(),
                        tradedQuantity,
                        tradePrice,
                        latencyMicros
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
    public synchronized boolean cancelOrder(String orderId) {
        boolean removed = buyOrders.removeIf(o -> o.getOrderId().equals(orderId));
        if (!removed) {
            removed = sellOrders.removeIf(o -> o.getOrderId().equals(orderId));
        }
        if (removed) {
            updateSnapshots();
            if (messagingTemplate != null) {
                Map<String, Object> payload = Map.of(
                        "bids", topBidsSnapshot,
                        "asks", topAsksSnapshot,
                        "timestamp", System.currentTimeMillis()
                );
                messagingTemplate.convertAndSend("/topic/orderbook", payload);
            }
            if (orderPersistenceService != null) {
                orderPersistenceService.cancelOrder(orderId);
            }
        }
        return removed;
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
    public long getTotalOrdersProcessed() {
        return totalOrdersProcessed.get();
    }
    public long getTotalTradesExecuted() {
        return totalTradesExecuted.get();
    }

    public long getLastMatchLatencyMicros() {
        return lastMatchLatencyMicros;
    }
    public double getTotalTradingVolume() {
        return totalTradingVolume;
    }
    public double getLastTradedPrice() {
        return lastTradedPrice;
    }
    public synchronized void restoreRestingOrdersFromDb(List<com.rest.entity.TradeOrder> orders) {
        if (orders == null || orders.isEmpty()) return;
        for (com.rest.entity.TradeOrder o : orders) {
            if (o.getRemainingQuantity() > 0) {
                OrderRequest req = new OrderRequest(
                        o.getOrderId(),
                        o.getSide(),
                        o.getOrderType(),
                        o.getPrice(),
                        o.getRemainingQuantity(),
                        o.getSequence(),
                        o.getCreatedAt() != null ? o.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : System.currentTimeMillis(),
                        o.getCustomer() != null ? o.getCustomer().getId() : null
                );

                if (o.getSide() == OrderSide.BUY) {
                    buyOrders.add(req);
                } else if (o.getSide() == OrderSide.SELL) {
                    sellOrders.add(req);
                }
            }
        }
        updateSnapshots();
        log.info("[OrderFlow Engine] Restored orders from MySQL. Current Book: {} Bids, {} Asks.",
                buyOrders.size(), sellOrders.size());
    }
    public synchronized void reset() {
        buyOrders.clear();
        sellOrders.clear();
        topBidsSnapshot = Collections.emptyList();
        topAsksSnapshot = Collections.emptyList();
    }
}


