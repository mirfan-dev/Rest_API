package com.rest.engine;


import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.rest.dtos.OrderEvent;
import com.rest.enums.OrderSide;
import org.springframework.stereotype.Component;
@Component
public class SecureOrderPublisher {
    private final RingBuffer<OrderEvent> ringBuffer;
    public SecureOrderPublisher(Disruptor<OrderEvent> disruptor) {
        this.ringBuffer = disruptor.getRingBuffer();
    }
    public void publishOrder(String orderId, OrderSide side, double price, int quantity) {
        long sequence = ringBuffer.next();
        try {
            OrderEvent event = ringBuffer.get(sequence);
            event.set(orderId, side, price, quantity);
        } finally {
            ringBuffer.publish(sequence);
        }
    }
}

