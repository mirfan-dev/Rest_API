package com.rest.service.impl;

import com.rest.dtos.CustomerDtos;
import com.rest.entity.Customer;
import com.rest.entity.Role;
import com.rest.repository.CustomerRepository;
import com.rest.repository.RoleRepository;
import com.rest.service.AuthService;
import com.rest.service.CustomerService;
import com.rest.service.EmailService;
import com.rest.util.AppConstant;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        Customer customer = mapper.map(customerDto, Customer.class);

        customer.setPassword(passwordEncoder.encode(customerDto.getPassword()));

        if (customer.getRoles() == null || customer.getRoles().isEmpty()) {
            Role defaultRole = roleRepository.findFirstByRoleName("ROLE_" + AppConstant.GUEST_ROLE)
                    .orElseThrow(() -> new RuntimeException("Default role not found"));
            customer.getRoles().add(defaultRole);
        }
        Customer savedCustomer = customerRepository.save(customer);
        // Send OTP after customer is saved
        authService.sendOtp(savedCustomer.getEmail());
        return mapper.map(savedCustomer, CustomerDtos.class);
    }
    @Override
    public CustomerDtos getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id) .orElseThrow(() -> new RuntimeException("Customer not found"));
        return mapper.map(customer, CustomerDtos.class);
    }


    @Override
    public List<CustomerDtos> getAllCustomers() {
        List<Customer> customers = customerRepository.findAll();
        return customers.stream() .map(customer -> mapper.map(customer, CustomerDtos.class)) .toList();
    }

    @Override
    public CustomerDtos updateCustomer(Long id, CustomerDtos customerDto) {
        Customer customer = customerRepository.findById(id) .orElseThrow(() -> new RuntimeException("Customer not found"));
        customer.setName(customerDto.getName());
        customer.setEmail(customerDto.getEmail());
        Customer updatedCustomer = customerRepository.save(customer);
        return mapper.map(updatedCustomer, CustomerDtos.class);
    }

    @Override
    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id) .orElseThrow(() -> new RuntimeException("Customer not found"));
        customerRepository.delete(customer);
    }
}
