package com.example.customer_service_integration_test.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerData {
    private Long customerId;
    private String name;
    private String address;
    private String email;
    private boolean status;
}
