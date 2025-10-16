package com.example.customer_service_integration_test.repository;

import com.example.customer_service_integration_test.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {
    
    /**
     * Find customer by customer ID
     * @param customerId the customer ID to search for
     * @return Optional containing the customer if found
     */
    Optional<CustomerEntity> findByCustomerId(Long customerId);
    
    /**
     * Find customer by email
     * @param email the email to search for
     * @return Optional containing the customer if found
     */
    Optional<CustomerEntity> findByEmail(String email);
    
    /**
     * Check if customer exists by customer ID
     * @param customerId the customer ID to check
     * @return true if customer exists, false otherwise
     */
    boolean existsByCustomerId(Long customerId);
}
