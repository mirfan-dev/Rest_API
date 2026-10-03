package com.rest.repository;

import com.rest.entity.Customer;
import com.rest.entity.TradeOrder;
import com.rest.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface OrderRepository extends JpaRepository<TradeOrder, Long> {

    Optional<TradeOrder> findByOrderId(String orderId);

    List<TradeOrder> findByCustomerOrderByCreatedAtDesc(Customer customer);

    List<TradeOrder> findTop50ByOrderByCreatedAtDesc();

    List<TradeOrder> findByStatusOrderByCreatedAtDesc(OrderStatus status);
}
