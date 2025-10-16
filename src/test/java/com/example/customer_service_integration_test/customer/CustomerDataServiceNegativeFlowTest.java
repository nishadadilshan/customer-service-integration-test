package com.example.customer_service_integration_test.customer;

import com.example.customer_service_integration_test.BaseTest;
import com.example.customer_service_integration_test.model.CustomerData;
import org.springframework.http.*;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class CustomerDataServiceNegativeFlowTest extends BaseTest {

    @BeforeClass
    public void setUp() {
        setup();
    }

    @DataProvider(name = "invalid_customer_data")
    public Object[][] invalidCustomerData() {
        return new Object[][]{
                // Invalid email format (missing @)
                {"testName", "Stockholm", "testgmail.com", true},
                // Invalid email format (missing domain)
                {"testName", "Stockholm", "test@", true},
                // Invalid email format (multiple @)
                {"testName", "Stockholm", "test@@gmail.com", true},
                // Empty name (should be required)
                {"", "Stockholm", "test@gmail.com", true},
                // Null name (should be required)
                {null, "Stockholm", "test@gmail.com", true},
                // Empty email (should be required)
                {"testName", "Stockholm", "", true},
                // Null email (should be required)
                {"testName", "Stockholm", null, true},
        };
    }


    @Test(dataProvider = "invalid_customer_data", invocationCount = 1, threadPoolSize = 1)
    public void test_invalid_customer_data(String name, String address, String email, boolean status) {
        // Create customer payload with invalid data
        CustomerData customerData = CustomerData.builder()
                .name(name)
                .address(address)
                .email(email)
                .status(status)
                .build();

        // Set headers using helper method
        HttpHeaders headers = createHeaders();

        // Create HTTP entity with customer JSON
        HttpEntity<CustomerData> request = new HttpEntity<>(customerData, headers);

        // Make POST request to create customer
        String createUrl = baseUrl + customerServiceEndPoint;

        // Method 1: Using RestTemplate with custom ErrorHandler (configured in setUp)
        ResponseEntity<Void> response = restTemplate.postForEntity(createUrl, request, Void.class);
        
        // Verify the response status
        Assert.assertEquals(response.getStatusCode(), HttpStatus.BAD_REQUEST, 
            "Expected 400 Bad Request for invalid data: " + email);
        
        System.out.println("✅ Correctly received 400 Bad Request for invalid data");
        
        // Verify that no customer was saved to database with this invalid email
        verifyCustomerNotSavedInDatabase(email);
        

    }
}
