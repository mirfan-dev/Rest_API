package com.rest.dtos;


import com.rest.enums.OrderSide;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {
    private String orderId;
    private OrderSide side;
    private double price;
    private int quantity;
    private long sequence;
    private long timestamp;


    public OrderRequest(String orderId, OrderSide side, double price, int quantity) {
        this(orderId, side, price, quantity, 0L, System.currentTimeMillis());
    }
    public OrderRequest(String orderId, OrderSide side, double price, int quantity, long sequence) {
        this(orderId, side, price, quantity, sequence, System.currentTimeMillis());
    }
}

