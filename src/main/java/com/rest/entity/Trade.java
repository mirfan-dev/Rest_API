package com.rest.entity;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "trades", indexes = {
        @Index(name = "idx_trade_id", columnList = "tradeId", unique = true),
        @Index(name = "idx_buy_order_id", columnList = "buyOrderId"),
        @Index(name = "idx_sell_order_id", columnList = "sellOrderId"),
        @Index(name = "idx_executed_at", columnList = "executedAt")
})
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 64)
    private String tradeId;
    @Column(nullable = false, length = 64)
    private String buyOrderId;
    @Column(nullable = false, length = 64)
    private String sellOrderId;

    private Long buyerCustomerId;
    private Long sellerCustomerId;
    @Column(nullable = false)
    private double price;

    private int quantity;
    @Column(nullable = false)
    private double totalAmount;
    private long executionLatencyMicros;
    @Column(nullable = false)
    private LocalDateTime executedAt;
    @PrePersist
    public void prePersist() {
        if (executedAt == null) {
            executedAt = LocalDateTime.now();
        }
        if (totalAmount <= 0) {
            totalAmount = price * quantity;
        }
    }
}
