package com.rest.engine;
import com.lmax.disruptor.EventFactory;
import com.rest.dtos.OrderEvent;

public class OrderEventFactory implements EventFactory<OrderEvent> {
    @Override
    public OrderEvent newInstance() {
        return new OrderEvent();
    }
}
