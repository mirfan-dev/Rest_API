package com.rest.engine;


import com.rest.dtos.OrderEvent;
import com.rest.enums.OrderSide;
import com.rest.enums.OrderType;

/**
 * A fixed-size circular collection of reusable order event slots. Each write
 * advances to the next slot; once the last slot is reached, writing wraps to
 * slot zero and overwrites the oldest event.
 */
public final class SimpleOrderRingBuffer {
    private final OrderEvent[] slots;
    private int nextWriteIndex;
    private int size;
    /** Creates a buffer and preallocates every event slot. */
    public SimpleOrderRingBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than zero");
        }
        slots = new OrderEvent[capacity];
        for (int i = 0; i < capacity; i++) {
            slots[i] = new OrderEvent();
        }
    }
    /** Writes values using String side and returns that slot. */
    public OrderEvent write(String orderId, String side, String orderType, double price, int quantity) {
        OrderEvent slot = slots[nextWriteIndex];
        slot.set(orderId, side, orderType, price, quantity);
        nextWriteIndex = (nextWriteIndex + 1) % slots.length;
        if (size < slots.length) {
            size++;
        }
        return slot;
    }
    /** Writes values using OrderSide and OrderType enums and returns that slot. */
    public OrderEvent write(String orderId, OrderSide side, OrderType orderType, double price, int quantity) {
        OrderEvent slot = slots[nextWriteIndex];
        slot.set(orderId, side, orderType, price, quantity);
        nextWriteIndex = (nextWriteIndex + 1) % slots.length;
        if (size < slots.length) {
            size++;
        }
        return slot;
    }
    /** Returns an event by its logical index, oldest first; rejects absent entries. */
    public OrderEvent get(int logicalIndex) {
        if (logicalIndex < 0 || logicalIndex >= size) {
            throw new IndexOutOfBoundsException("No event at logical index " + logicalIndex);
        }
        int oldestIndex = (nextWriteIndex - size + slots.length) % slots.length;
        return slots[(oldestIndex + logicalIndex) % slots.length];
    }
    public int capacity() { return slots.length; }
    public int size() { return size; }
}
