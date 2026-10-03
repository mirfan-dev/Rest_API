package com.rest.dtos;


import com.rest.enums.OrderSide;
import com.rest.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {
    private String orderId;
    private OrderSide side;
    private OrderType orderType;
    private double price;
    private int quantity;
    private long sequence;
    private long timestamp;
    private Long customerId;

    public OrderRequest(String orderId, OrderSide side, double price, int quantity) {
        this(orderId, side, OrderType.LIMIT, price, quantity, 0L, System.currentTimeMillis(), null);
    }


    public OrderRequest(String orderId, OrderSide side, double price, int quantity, long sequence) {
        this(orderId, side, OrderType.LIMIT, price, quantity, sequence, System.currentTimeMillis(), null);
    }
    public OrderRequest(String orderId, OrderSide side, double price, int quantity, long sequence, long timestamp) {
        this(orderId, side, OrderType.LIMIT, price, quantity, sequence, timestamp, null);
    }
}
