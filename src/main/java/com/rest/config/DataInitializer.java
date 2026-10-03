package com.rest.config;

import com.rest.engine.OrderEventHandler;
import com.rest.entity.Role;
import com.rest.entity.TradeOrder;
import com.rest.enums.OrderStatus;
import com.rest.repository.CustomerRepository;
import com.rest.repository.OrderRepository;
import com.rest.repository.RoleRepository;
import com.rest.util.AppConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderEventHandler orderEventHandler;
    @Override
    public void run(ApplicationArguments args) {
        log.info("[OrderFlow] Initializing database roles and restoring persistent orders from database...");
        // 1. Ensure required system roles exist in 'roles' table for user registration
        roleRepository.findFirstByRoleName("ROLE_" + AppConstant.GUEST_ROLE)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .roleId(UUID.randomUUID().toString())
                        .roleName("ROLE_" + AppConstant.GUEST_ROLE)
                        .build()));

        roleRepository.findFirstByRoleName("ROLE_" + AppConstant.ADMIN_ROLE)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .roleId(UUID.randomUUID().toString())
                        .roleName("ROLE_" + AppConstant.ADMIN_ROLE)
                        .build()));
        roleRepository.findFirstByRoleName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .roleId(UUID.randomUUID().toString())
                        .roleName("ROLE_USER")
                        .build()));
        // 2. Restore all active resting orders from MySQL 'orders' table into matching engine memory
        List<TradeOrder> pendingOrders = orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.PENDING);
        List<TradeOrder> partialOrders = orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.PARTIALLY_FILLED);
        log.info("[OrderFlow DB] Found {} PENDING and {} PARTIALLY_FILLED orders in MySQL database.",
                pendingOrders.size(), partialOrders.size());
        orderEventHandler.restoreRestingOrdersFromDb(pendingOrders);
        orderEventHandler.restoreRestingOrdersFromDb(partialOrders);
        log.info("[OrderFlow] System ready. All data is 100% sourced from MySQL database.");
    }
}
