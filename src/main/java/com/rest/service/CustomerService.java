package com.rest.service;

import com.rest.dtos.CustomerDtos;
import com.rest.entity.Customer;

import java.util.List;

public interface CustomerService {

    CustomerDtos createCustomer(CustomerDtos customer);

    CustomerDtos getCustomerById(Long id);

    List<CustomerDtos> getAllCustomers();

    CustomerDtos updateCustomer(Long id, CustomerDtos customer);

    void deleteCustomer(Long id);
}
