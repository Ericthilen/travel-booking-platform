package com.ericthilen.travelbookingplatform.repository;

import com.ericthilen.travelbookingplatform.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository
        extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPersonalNumber(
            String personalNumber
    );

    Optional<Customer> findByUser(com.ericthilen.travelbookingplatform.model.User user);

    boolean existsByCustomerNumber(
            String customerNumber
    );
}