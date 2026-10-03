package com.rest.service;

import com.rest.entity.Customer;
import com.rest.entity.Trade;
import com.rest.entity.TradeOrder;
import com.rest.enums.OrderSide;
import com.rest.enums.OrderType;
import java.util.List;
import java.util.Optional;
public interface OrderPersistenceService {


    TradeOrder createAndSaveOrder(String orderId, OrderSide side, OrderType orderType, double price, int quantity, Customer customer);

    void recordTradeExecution(String buyOrderId, String sellOrderId, int quantity, double price, long executionLatencyMicros);

    boolean cancelOrder(String orderId);

    Optional<TradeOrder> getOrderByOrderId(String orderId);

    List<TradeOrder> getOrdersByCustomer(Customer customer);

    List<TradeOrder> getRecentOrders();

    List<Trade> getRecentTrades();

    List<Trade> getTradesByCustomer(Long customerId);
}