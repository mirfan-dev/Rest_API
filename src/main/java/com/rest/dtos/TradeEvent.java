package com.rest.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TradeEvent {
    private String tradeId;
    private String buyOrderId;
    private String sellOrderId;
    private int quantity;
    private double price;
    private long timestamp;

    public TradeEvent(String buyOrderId, String sellOrderId, int quantity, double price) {
        this("TRD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                buyOrderId, sellOrderId, quantity, price, System.currentTimeMillis());
    }
}
