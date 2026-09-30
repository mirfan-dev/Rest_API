package com.rest.entity;

import com.rest.enums.OrderSide;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Data
@Entity
@Table(name = "orders")
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String orderId;
    @Enumerated(EnumType.STRING)
    private OrderSide side;
    private double price;
    private int quantity;
    private long sequence;
    private LocalDateTime timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;   // ← many orders → one customer
}
