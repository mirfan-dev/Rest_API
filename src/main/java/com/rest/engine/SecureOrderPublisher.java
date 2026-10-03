package com.rest.engine;


import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.rest.dtos.OrderEvent;
import com.rest.enums.OrderSide;
import com.rest.enums.OrderType;
import org.springframework.stereotype.Component;
@Component
public class SecureOrderPublisher {
    private final RingBuffer<OrderEvent> ringBuffer;
    public SecureOrderPublisher(Disruptor<OrderEvent> disruptor) {
        this.ringBuffer = disruptor.getRingBuffer();
    }
    public void publishOrder(String orderId, OrderSide side, double price, int quantity) {
        publishOrder(orderId, side, OrderType.LIMIT, price, quantity, null);
    }
    public void publishOrder(String orderId, OrderSide side, OrderType orderType, double price, int quantity, Long customerId) {
        long sequence = ringBuffer.next();
        try {
            OrderEvent event = ringBuffer.get(sequence);
            event.set(orderId, side, orderType, price, quantity, customerId);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    public long getCursor() {
        return ringBuffer.getCursor();
    }
    public int getBufferSize() {
        return ringBuffer.getBufferSize();
    }
    public long getRemainingCapacity() {
        return ringBuffer.remainingCapacity();
    }
}

