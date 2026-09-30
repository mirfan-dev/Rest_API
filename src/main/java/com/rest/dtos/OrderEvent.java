package com.rest.dtos;

import com.rest.enums.OrderSide;
import com.rest.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderEvent {
    private String orderId;
    private OrderSide side;
    private OrderType orderType;
    private double price;
    private int quantity;
    private long timestamp;

    // Overload 1: String side and String orderType
    public void set(String orderId, String side, String orderType, double price, int quantity) {
        this.orderId = orderId;
        this.side = side != null ? OrderSide.valueOf(side.toUpperCase()) : null;
        this.orderType = orderType != null ? OrderType.valueOf(orderType.toUpperCase()) : OrderType.LIMIT;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = System.currentTimeMillis();
    }

    // Overload 2: OrderSide enum and OrderType enum
    public void set(String orderId, OrderSide side, OrderType orderType, double price, int quantity) {
        this.orderId = orderId;
        this.side = side;
        this.orderType = orderType != null ? orderType : OrderType.LIMIT;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = System.currentTimeMillis();
    }

    // Overload 3: OrderSide enum and default to LIMIT orderType
    public void set(String orderId, OrderSide side, double price, int quantity) {
        set(orderId, side, OrderType.LIMIT, price, quantity);
    }
}