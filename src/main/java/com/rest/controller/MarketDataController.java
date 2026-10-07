package com.rest.controller;

import com.rest.dtos.TradeEvent;
import com.rest.engine.OrderEventHandler;
import com.rest.engine.SecureOrderPublisher;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.rest.entity.Trade;
import com.rest.repository.TradeRepository;
import java.io.IOException;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
@RestController
public class MarketDataController {
    private final OrderEventHandler orderEventHandler;
    private final SecureOrderPublisher publisher;
    private final TradeRepository tradeRepository;
    public MarketDataController(OrderEventHandler orderEventHandler, SecureOrderPublisher publisher, TradeRepository tradeRepository) {
        this.orderEventHandler = orderEventHandler;
        this.publisher = publisher;
        this.tradeRepository = tradeRepository;
    }

    @GetMapping("/api/orderbook")
    public Map<String, Object> getLiveOrderBook() {
        return Map.of(
                "status", "Live",
                "bids", orderEventHandler.getTopBids(),
                "asks", orderEventHandler.getTopAsks(),
                "lastTradedPrice", orderEventHandler.getLastTradedPrice(),
                "timestamp", System.currentTimeMillis()
        );
    }
    @GetMapping("/api/marketdata/stats")
    public Map<String, Object> getEngineStats() {
        Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("status", "ONLINE");
        stats.put("engine", "LMAX Disruptor Lock-Free RingBuffer");
        stats.put("ringBufferSize", publisher.getBufferSize());
        stats.put("ringBufferCursor", publisher.getCursor());
        stats.put("remainingCapacity", publisher.getRemainingCapacity());
        stats.put("totalOrdersProcessed", orderEventHandler.getTotalOrdersProcessed());
        stats.put("totalTradesExecuted", orderEventHandler.getTotalTradesExecuted());
        stats.put("lastMatchLatencyMicros", orderEventHandler.getLastMatchLatencyMicros());
        stats.put("totalTradingVolume", orderEventHandler.getTotalTradingVolume());
        stats.put("lastTradedPrice", orderEventHandler.getLastTradedPrice());
        stats.put("timestamp", System.currentTimeMillis());
        return stats;
    }

    @GetMapping(value = "/api/marketdata/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMarketData() {
        // SSE connection with 30-minute timeout
        SseEmitter emitter = new SseEmitter(1800000L);
        Consumer<Map<String, Object>> obListener = payload -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("orderbook")
                        .data(payload));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        };
        Consumer<TradeEvent> tradeListener = trade -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("trade")
                        .data(trade));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        };

        orderEventHandler.addOrderBookListener(obListener);
        orderEventHandler.addTradeListener(tradeListener);
        emitter.onCompletion(() -> {
            orderEventHandler.removeOrderBookListener(obListener);
            orderEventHandler.removeTradeListener(tradeListener);
        });
        emitter.onTimeout(() -> {
            orderEventHandler.removeOrderBookListener(obListener);
            orderEventHandler.removeTradeListener(tradeListener);
            emitter.complete();
        });
        emitter.onError(e -> {
            orderEventHandler.removeOrderBookListener(obListener);
            orderEventHandler.removeTradeListener(tradeListener);
        });
        // Send initial snapshot
        try {
            emitter.send(SseEmitter.event()
                    .name("orderbook")
                    .data(Map.of(
                            "bids", orderEventHandler.getTopBids(),
                            "asks", orderEventHandler.getTopAsks(),
                            "timestamp", System.currentTimeMillis()
                    )));
        } catch (IOException ignored) {}

        return emitter;
    }
    @GetMapping("/api/marketdata/candles")
    public List<Map<String, Object>> getRealCandles() {
        List<Trade> trades = tradeRepository.findAll();
        if (trades.isEmpty()) {
            return Collections.emptyList();
        }
        // Sort trades chronologically from database
        trades.sort(Comparator.comparing(Trade::getExecutedAt));
        List<Map<String, Object>> candles = new ArrayList<>();
        Map<Long, List<Trade>> bucketed = new TreeMap<>();
        for (Trade t : trades) {
            long epochMilli = t.getExecutedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long bucketTime = (epochMilli / 60000L) * 60000L;
            bucketed.computeIfAbsent(bucketTime, k -> new ArrayList<>()).add(t);
        }

        for (Map.Entry<Long, List<Trade>> entry : bucketed.entrySet()) {
            List<Trade> bucketTrades = entry.getValue();
            double open = bucketTrades.get(0).getPrice();
            double close = bucketTrades.get(bucketTrades.size() - 1).getPrice();
            double high = bucketTrades.stream().mapToDouble(Trade::getPrice).max().orElse(open);
            double low = bucketTrades.stream().mapToDouble(Trade::getPrice).min().orElse(open);
            long volume = bucketTrades.stream().mapToLong(Trade::getQuantity).sum();
            Map<String, Object> candle = new LinkedHashMap<>();
            candle.put("time", entry.getKey());
            candle.put("open", open);
            candle.put("high", high);
            candle.put("low", low);
            candle.put("close", close);
            candle.put("volume", volume);
            candles.add(candle);
        }
        return candles;
    }
}

