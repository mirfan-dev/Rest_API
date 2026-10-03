package com.rest.repository;


import com.rest.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {
    Optional<Trade> findByTradeId(String tradeId);
    List<Trade> findTop50ByOrderByExecutedAtDesc();
    List<Trade> findByBuyOrderIdOrSellOrderId(String buyOrderId, String sellOrderId);
    List<Trade> findByBuyerCustomerIdOrSellerCustomerIdOrderByExecutedAtDesc(Long buyerCustomerId, Long sellerCustomerId);
}
