package com.rest.controller;

import com.rest.dtos.CustomerDtos;
import com.rest.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;


                // Create Customer + // Send OTP

                 @PostMapping
                 public ResponseEntity<CustomerDtos> createCustomer( @RequestBody CustomerDtos customerDto) {
                     CustomerDtos savedCustomer = customerService.createCustomer(customerDto);
                     return new ResponseEntity<>( savedCustomer, HttpStatus.CREATED );

                 }

                 // Get Customer by ID
                 @GetMapping("/{id}")
                 public ResponseEntity<CustomerDtos> getCustomerById( @PathVariable Long id) {
                     CustomerDtos customer = customerService.getCustomerById(id);
                     return ResponseEntity.ok(customer);

                 }

                 // Get All Customers
                 @GetMapping
                 public ResponseEntity<List<CustomerDtos>> getAllCustomers() {
                     List<CustomerDtos> customers = customerService.getAllCustomers();
                     return ResponseEntity.ok(customers);
                 }

                 // Update Customer
                @PutMapping("/{id}")
                public ResponseEntity<CustomerDtos> updateCustomer( @PathVariable Long id, @RequestBody CustomerDtos customerDto) {
                     CustomerDtos updatedCustomer = customerService.updateCustomer(id, customerDto);
                     return ResponseEntity.ok(updatedCustomer);
                 }
                 // Delete Customer
                @DeleteMapping("/{id}") public ResponseEntity<String> deleteCustomer( @PathVariable Long id) {
                     customerService.deleteCustomer(id); return ResponseEntity.ok("Customer deleted successfully");
                 }

     }
