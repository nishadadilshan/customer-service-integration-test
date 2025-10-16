package com.example.customer_service_integration_test.customer;

import com.example.customer_service_integration_test.entity.CustomerEntity;
import com.example.customer_service_integration_test.model.Customer;
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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class CustomerServiceFlowTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private CustomerRepository customerRepository;

    // Fields from BaseTest
    protected RestTemplate restTemplate;
    protected ObjectMapper objectMapper;
    protected String baseUrl;
    protected String createEndpoint;

    protected String updateEndpoint;
    protected Properties testProperties;


    @DataProvider(name = "customer_data")
    public Object[][] customerData() {
        return new Object[][]{
                {"testName", "Stockholm", "test@gmail.com", true},
        };
    }

    @DataProvider(name = "customer_update_data")
    public Object[][] customerUpdateData() {
        return new Object[][]{
                {"Updated name", "updated Stockholm", "updated@gmail.com", true},
        };
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



    @BeforeClass
    public void setUp() {
        restTemplate = new RestTemplate();
        
        // Configure RestTemplate to not throw exceptions for 4xx/5xx responses
        restTemplate.setErrorHandler(new org.springframework.web.client.DefaultResponseErrorHandler() {
            @Override
            public void handleError(java.net.URI url, org.springframework.http.HttpMethod method, 
                                  org.springframework.http.client.ClientHttpResponse response) throws java.io.IOException {
                // Don't throw exception, let the response be handled normally
            }
        });
        
        objectMapper = new ObjectMapper();
        loadTestProperties();
        baseUrl = getTestProperty("api.base.url", "http://localhost:8081");
        createEndpoint = getTestProperty("api.customer.create.endpoint", "/api/customer/create");
        updateEndpoint = getTestProperty("api.customer.create.endpoint", "/api/customer/update");
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
            testProperties.setProperty("api.customer.create.endpoint", "/api/customer/create");
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

    @Test(dataProvider = "invalid_customer_data", invocationCount = 1, threadPoolSize = 1)
    public void test_invalid_customer_data(String name, String address, String email, boolean status) {
        // Create customer payload with invalid data
        Customer customer = Customer.builder()
                .name(name)
                .address(address)
                .email(email)
                .status(status)
                .build();

        // Set headers using helper method
        HttpHeaders headers = createHeaders();

        // Create HTTP entity with customer JSON
        HttpEntity<Customer> request = new HttpEntity<>(customer, headers);

        // Make POST request to create customer
        String createUrl = baseUrl + createEndpoint;

        // Method 1: Using RestTemplate with custom ErrorHandler (configured in setUp)
        ResponseEntity<Void> response = restTemplate.postForEntity(createUrl, request, Void.class);
        
        // Verify the response status
        Assert.assertEquals(response.getStatusCode(), HttpStatus.BAD_REQUEST, 
            "Expected 400 Bad Request for invalid data: " + email);
        
        System.out.println("✅ Correctly received 400 Bad Request for invalid data");
        
        // Verify that no customer was saved to database with this invalid email
        verifyCustomerNotSavedInDatabase(email);
        

    }

    // Alternative test method using TestRestTemplate (Spring Boot test utility)
    @Test(dataProvider = "invalid_customer_data", invocationCount = 1, threadPoolSize = 1, enabled = false)
    public void test_invalid_customer_data_with_test_rest_template(String name, String address, String email, boolean status) {
        // Create customer payload with invalid data
        Customer customer = Customer.builder()
                .name(name)
                .address(address)
                .email(email)
                .status(status)
                .build();

        // Set headers using helper method
        HttpHeaders headers = createHeaders();
        HttpEntity<Customer> request = new HttpEntity<>(customer, headers);

        // Make POST request to create customer
        String createUrl = baseUrl + createEndpoint;

        // Method 2: Using TestRestTemplate (Spring Boot test utility)
        org.springframework.boot.test.web.client.TestRestTemplate testRestTemplate =
            new org.springframework.boot.test.web.client.TestRestTemplate();

        ResponseEntity<Void> response = testRestTemplate.postForEntity(createUrl, request, Void.class);

        // Verify the response status
        Assert.assertEquals(response.getStatusCode(), HttpStatus.BAD_REQUEST,
            "Expected 400 Bad Request for invalid data: " + email);

        System.out.println("✅ TestRestTemplate correctly received 400 Bad Request for invalid data");

        // Verify that no customer was saved to database with this invalid email
        verifyCustomerNotSavedInDatabase(email);
    }

    @Test(dataProvider = "customer_data", invocationCount = 1, threadPoolSize = 1)
    public void testCreateCustomerApi(String name, String address, String email, boolean status) {
        try {
            // Create customer payload as JSON string using properties
            Customer customer = Customer.builder()
                    .name(name)
                    .address(address)
                    .email(email)
                    .status(status)
                    .build();

            // Set headers using helper method
            HttpHeaders headers = createHeaders();

            // Create HTTP entity with customer JSON
            HttpEntity<Customer> request = new HttpEntity<>(customer, headers);

            // Make POST request to create customer
            String createUrl = baseUrl + createEndpoint;

            //SAVE customer object here
            ResponseEntity<Customer> response = restTemplate.postForEntity(createUrl, request, Customer.class);

            // Verify response

            Assert.assertEquals(response.getStatusCode(), HttpStatus.OK, "API should return 200 OK");
            Assert.assertNotNull(response.getBody(), "Response body should not be null");

            // Parse response to verify customer was created
            Customer savedCustomer = response.getBody();

            // Verify the values match what we sent
            Assert.assertEquals(savedCustomer.getName(), customer.getName(), "Customer name should match");
            Assert.assertEquals(savedCustomer.getEmail(), customer.getEmail(), "Customer email should match");
            Assert.assertEquals(savedCustomer.getAddress(), customer.getAddress(), "Customer address should match");
            Assert.assertEquals(savedCustomer.isStatus(), customer.isStatus(), "Customer status should be true");

            // Verify customerId was auto-generated (not 0)
            long customerId = savedCustomer.getCustomerId();
            Assert.assertNotEquals(customerId, 0, "Customer ID should be auto-generated");

            System.out.println("Customer created successfully with ID: " + customerId);
            System.out.println("Response: " + response.getBody());

            verifyCustomerDataSavedInDatabase(customerId, customer);


            //update the customer object and verify here
            //updat api call and verify


            //delete customer here
            customerRepository.deleteById(customerId);//newed to call delete api here
            //Optional<CustomerEntity> customerEntity = customerRepository.getReferenceById(customerId);


        } catch (Exception e) {
            Assert.fail("Test failed with exception:--->> " + e.getMessage(), e);
        }
    }

    @Test(dataProvider = "customer_update_data", invocationCount = 1, threadPoolSize = 1)
    public void test_update_customer_data(String name, String address, String email, boolean status){
        Customer customer = Customer.builder()
                .name(name)
                .address(address)
                .email(email)
                .status(status)
                .build();

        HttpHeaders headers = createHeaders();

        // Create HTTP entity with customer JSON
        HttpEntity<Customer> request = new HttpEntity<>(customer, headers);

        // Make POST request to update customer
        String createUrl = baseUrl + updateEndpoint;

        //update customer object here
        ResponseEntity<Customer> response = restTemplate.postForEntity(createUrl, request, Customer.class);

        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK, "API Should return 200");
        Assert.assertNotNull(response.getBody(), " Response body should not be null");

        Customer updatedCustomer = response.getBody();

        // Verify the values match what we sent
        Assert.assertEquals(updatedCustomer.getName(), customer.getName(), "Customer name should match");
        Assert.assertEquals(updatedCustomer.getEmail(), customer.getEmail(), "Customer email should match");
        Assert.assertEquals(updatedCustomer.getAddress(), customer.getAddress(), "Customer address should match");
        Assert.assertEquals(updatedCustomer.isStatus(), customer.isStatus(), "Customer status should be true");

        // Verify customerId was auto-generated (not 0)
        long customerId = updatedCustomer.getCustomerId();
        Assert.assertNotEquals(customerId, 0, "Customer ID should be auto-generated");

        System.out.println("Customer created successfully with ID: " + customerId);
        System.out.println("Response: " + response.getBody());

        verifyCustomerDataSavedInDatabase(customerId, customer);

    }

    /**
     * Verifies that customer data is properly saved in the database and compares with original data
     *
     * @param customerId       the customer ID to verify
     * @param originalCustomer the original customer data that was sent to the API
     */
    private void verifyCustomerDataSavedInDatabase(Long customerId, Customer originalCustomer) {
        try {
            System.out.println("Verifying customer data in database for ID: " + customerId);

            // Find the customer in the database
            Optional<CustomerEntity> customerEntity = customerRepository.findByCustomerId(customerId);

            // Verify customer exists in database
            Assert.assertTrue(customerEntity.isPresent(),
                    "Customer with ID " + customerId + " should exist in database");

            CustomerEntity savedCustomer = customerEntity.get();


            // Verify customer data matches what was sent
            Assert.assertEquals(savedCustomer.getName(), originalCustomer.getName(),
                    "Customer name in database should match original name");
            Assert.assertEquals(savedCustomer.getEmail(), originalCustomer.getEmail(),
                    "Customer email in database should match original email");
            Assert.assertEquals(savedCustomer.getAddress(), originalCustomer.getAddress(),
                    "Customer address in database should match original address");
            Assert.assertEquals(savedCustomer.isStatus(), originalCustomer.isStatus(),
                    "Customer status in database should match original status");

            System.out.println("✅ Database verification successful - All data matches!");

        } catch (Exception e) {
            Assert.fail("Database verification failed for customer ID " + customerId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Verifies that customer data was NOT saved in the database (for invalid data tests)
     *
     * @param email the email to check for in the database
     */
    private void verifyCustomerNotSavedInDatabase(String email) {
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
}
