package com.rest.engine;

import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.rest.dtos.OrderEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Configuration
public class DisruptorConfig {

    @Bean
    public Disruptor<OrderEvent> orderEventDisruptor(
            OrderEventHandler orderEventHandler) {

        ThreadFactory threadFactory = Executors.defaultThreadFactory();

        Disruptor<OrderEvent> disruptor = new Disruptor<>(
                new OrderEventFactory(),
                1024,
                threadFactory,
                ProducerType.MULTI,
                new BusySpinWaitStrategy()
        );

        disruptor.handleEventsWith(orderEventHandler);

        disruptor.start();

        return disruptor;
    }
}