package com.example.customer_service_integration_test;

import com.example.customer_service_integration_test.entity.CustomerEntity;
import com.example.customer_service_integration_test.model.CustomerData;
import com.example.customer_service_integration_test.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.web.client.RestTemplate;
import org.testng.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class BaseTest extends AbstractTestNGSpringContextTests {
    @Autowired
    protected CustomerRepository customerRepository;

    // Fields from BaseTest
    protected RestTemplate restTemplate;
    protected ObjectMapper objectMapper;
    protected String baseUrl;
    protected String customerServiceEndPoint;

    protected Properties testProperties;

    protected void setup() {
        restTemplate = new RestTemplate();
        // Configure RestTemplate to not throw exceptions for 4xx/5xx responses
        restTemplate.setErrorHandler(new org.springframework.web.client.DefaultResponseErrorHandler() {
            @Override
            public void handleError(java.net.URI url, HttpMethod method,
                                    org.springframework.http.client.ClientHttpResponse response) throws IOException {
                // Don't throw exception, let the response be handled normally
            }
        });
        objectMapper = new ObjectMapper();
        loadTestProperties();
        baseUrl = getTestProperty("api.base.url", "http://localhost:8081");
        customerServiceEndPoint = getTestProperty("api.customer.service.endpoint", "/api/customer");
    }

    protected void loadTestProperties() {
        testProperties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application-test.properties")) {
            if (input != null) {
                testProperties.load(input);
            } else {
                // Fallback to default values if properties file is not found
                testProperties.setProperty("api.base.url", "http://localhost:8081");
                testProperties.setProperty("api.customer.create.endpoint", "/api/customer/create");
            }
        } catch (IOException e) {
            System.err.println("Error loading test properties: " + e.getMessage());
            // Use default values
            testProperties.setProperty("api.base.url", "http://localhost:8081");
            testProperties.setProperty("api.customer.service.endpoint", "/api/customer");
        }
    }

    protected HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("accept", "application/json");
        return headers;
    }

    protected String getTestProperty(String key, String defaultValue) {
        return testProperties.getProperty(key, defaultValue);
    }

    /**
     * Verifies that customer data was NOT saved in the database (for invalid data tests)
     *
     * @param email the email to check for in the database
     */
    protected void verifyCustomerNotSavedInDatabase(String email) {
        try {
            System.out.println("Verifying customer with email '" + email + "' was NOT saved in database");

            // Try to find the customer in the database by email
            Optional<CustomerEntity> customerEntity = customerRepository.findByEmail(email);

            // Verify customer does NOT exist in database
            Assert.assertFalse(customerEntity.isPresent(),
                    "Customer with email '" + email + "' should NOT exist in database (invalid data should be rejected)");

            System.out.println("✅ Database verification successful - Invalid customer was correctly rejected!");

        } catch (Exception e) {
            Assert.fail("Database verification failed for email " + email + ": " + e.getMessage(), e);
        }
    }
    protected ResponseEntity<CustomerData> createCustomer(CustomerData customerData) {
        // Set headers using helper method
        HttpHeaders headers = createHeaders();

        // Create HTTP entity with customer JSON
        HttpEntity<CustomerData> request = new HttpEntity<>(customerData, headers);

        // Make POST request to create customer
        String createUrl = baseUrl + customerServiceEndPoint;

        //SAVE customer object here
        return restTemplate.postForEntity(createUrl, request, CustomerData.class);
    }

    protected ResponseEntity<CustomerData> updateCustomer(CustomerData savedCustomerData) {
        // Set headers using helper method
        HttpHeaders headers = createHeaders();

        // Create HTTP entity with customer JSON
        HttpEntity<CustomerData> request = new HttpEntity<>(savedCustomerData, headers);

        // Make POST request to create customer
        String createUrl = baseUrl + customerServiceEndPoint+ "/"+ savedCustomerData.getCustomerId();

        //Update customer object here
        return restTemplate.exchange(createUrl,HttpMethod.PUT,request, CustomerData.class);
    }

    protected ResponseEntity<CustomerData> deleteCustomer(CustomerData savedCustomerData) {
        // Set headers using helper method
        HttpHeaders headers = createHeaders();

        // Create HTTP entity with customer JSON
        HttpEntity<CustomerData> request = new HttpEntity<>(savedCustomerData, headers);

        // Make POST request to create customer
        String createUrl = baseUrl + customerServiceEndPoint+ "/"+ savedCustomerData.getCustomerId();

        //Delete customer object here
        return restTemplate.exchange(createUrl,HttpMethod.DELETE,request, CustomerData.class);
    }

    protected ResponseEntity<CustomerData> getCustomer(CustomerData savedCustomerData) {
        // Set headers using helper method
        HttpHeaders headers = createHeaders();

        // Create HTTP entity with customer JSON
        HttpEntity<CustomerData> request = new HttpEntity<>(savedCustomerData, headers);

        // Make POST request to create customer
        String createUrl = baseUrl + customerServiceEndPoint+ "/"+ savedCustomerData.getCustomerId();

        //Delete customer object here
        return restTemplate.exchange(createUrl,HttpMethod.GET,request, CustomerData.class);
    }

    /**
     * Verifies that customer data is properly saved in the database and compares with original data
     *
     * @param customerId       the customer ID to verify
     * @param originalCustomerData the original customer data that was sent to the API
     */
    protected void verifyCustomerDataSavedInDatabase(Long customerId, CustomerData originalCustomerData) {
        try {
            System.out.println("Verifying customer data in database for ID: " + customerId);

            // Find the customer in the database
            Optional<CustomerEntity> customerEntity = customerRepository.findByCustomerId(customerId);

            // Verify customer exists in database
            Assert.assertTrue(customerEntity.isPresent(),
                    "Customer with ID " + customerId + " should exist in database");

            CustomerEntity savedCustomer = customerEntity.get();


            // Verify customer data matches what was sent
            Assert.assertEquals(savedCustomer.getName(), originalCustomerData.getName(),
                    "Customer name in database should match original name");
            Assert.assertEquals(savedCustomer.getEmail(), originalCustomerData.getEmail(),
                    "Customer email in database should match original email");
            Assert.assertEquals(savedCustomer.getAddress(), originalCustomerData.getAddress(),
                    "Customer address in database should match original address");
            Assert.assertEquals(savedCustomer.isStatus(), originalCustomerData.isStatus(),
                    "Customer status in database should match original status");

            System.out.println("✅ Database verification successful - All data matches!");

        } catch (Exception e) {
            Assert.fail("Database verification failed for customer ID " + customerId + ": " + e.getMessage(), e);
        }
    }
}
