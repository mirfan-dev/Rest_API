package com.rest.service.impl;

import com.rest.dtos.CustomerDtos;
import com.rest.entity.Customer;
import com.rest.entity.Role;
import com.rest.repository.CustomerRepository;
import com.rest.repository.RoleRepository;
import com.rest.service.AuthService;
import com.rest.service.CustomerService;
import com.rest.util.AppConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;
    private final ModelMapper mapper;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;


    @Override
    @Transactional
    public CustomerDtos createCustomer(CustomerDtos customerDto) {
        if (customerRepository.findByEmail(customerDto.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists: " + customerDto.getEmail());
        }
        Customer customer = mapper.map(customerDto, Customer.class);
        customer.setPassword(passwordEncoder.encode(customerDto.getPassword()));
        if (customer.getRoles() == null) {
            customer.setRoles(new HashSet<>());
        }
        if (customer.getRoles().isEmpty()) {
            Role defaultRole = roleRepository.findFirstByRoleName("ROLE_USER")
                    .orElseGet(() -> roleRepository.findFirstByRoleName("ROLE_" + AppConstant.GUEST_ROLE)
                            .orElseGet(() -> roleRepository.save(Role.builder()
                                    .roleId(UUID.randomUUID().toString())
                                    .roleName("ROLE_USER")
                                    .build())));
            customer.getRoles().add(defaultRole);
        }
        Customer savedCustomer = customerRepository.save(customer);
        // Attempt OTP send gracefully without crashing if mail server is unconfigured
        try {
            authService.sendOtp(savedCustomer.getEmail());
        } catch (Exception e) {
            log.warn("[CustomerService] User created successfully, but OTP email dispatch skipped/failed: {}", e.getMessage());
        }
        return mapper.map(savedCustomer, CustomerDtos.class);
    }

    @Override
    public CustomerDtos getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        return mapper.map(customer, CustomerDtos.class);
    }
    @Override
    public List<CustomerDtos> getAllCustomers() {
        List<Customer> customers = customerRepository.findAll();
        return customers.stream()
                .map(customer -> mapper.map(customer, CustomerDtos.class))
                .toList();
    }

    @Override
    public CustomerDtos updateCustomer(Long id, CustomerDtos customerDto) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        customer.setName(customerDto.getName());
        customer.setEmail(customerDto.getEmail());
        if (customerDto.getPassword() != null && !customerDto.getPassword().isBlank()) {
            customer.setPassword(passwordEncoder.encode(customerDto.getPassword()));
        }
        Customer updatedCustomer = customerRepository.save(customer);
        return mapper.map(updatedCustomer, CustomerDtos.class);
    }
    @Override
    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        customerRepository.delete(customer);
    }
}


