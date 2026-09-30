package com.rest.service.impl;


import com.rest.dtos.TradeEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class OrderConsumer {
    @KafkaListener(
            topics = "orders-topic",
            groupId = "order-group"
    )
    public void consume(TradeEvent event) {
        System.out.printf("[Kafka Consumer] TRADE RECEIVED: BUY=%s | SELL=%s | Qty=%d | Price=$%.2f%n",
                event.getBuyOrderId(), event.getSellOrderId(), event.getQuantity(), event.getPrice());
    }
}
