package com.rest.service.impl;

import com.rest.dtos.TradeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
@Service

public class KafkaOrderProducer {
    private final KafkaTemplate<String, TradeEvent> kafkaTemplate;
    public KafkaOrderProducer(@Autowired(required = false) KafkaTemplate<String, TradeEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    public void publishTrade(
            String buyOrderId,
            String sellOrderId,
            int quantity,
            double price) {
        if (kafkaTemplate == null) {
            return;
        }
        TradeEvent tradeEvent = new TradeEvent(
                buyOrderId,
                sellOrderId,
                quantity,
                price
        );
        try {
            kafkaTemplate.send(
                    "orders-topic",
                    buyOrderId,
                    tradeEvent
            );
        } catch (Exception e) {
            System.err.println("[Kafka] Broker not reachable, skipping broadcast: " + e.getMessage());
        }
    }
}
