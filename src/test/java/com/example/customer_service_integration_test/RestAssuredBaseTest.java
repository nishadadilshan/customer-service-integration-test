package com.example.customer_service_integration_test;

import com.example.customer_service_integration_test.entity.CustomerEntity;
import com.example.customer_service_integration_test.model.CustomerData;
import com.example.customer_service_integration_test.repository.CustomerRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class RestAssuredBaseTest extends AbstractTestNGSpringContextTests {
    
    @Autowired
    protected CustomerRepository customerRepository;

    // Fields for RestAssured configuration
    protected String baseUrl;
    protected String customerServiceEndPoint;
    protected Properties testProperties;

    protected void setupRestAssured() {
        loadTestProperties();
        baseUrl = getTestProperty("api.base.url", "http://localhost:8081");
        customerServiceEndPoint = getTestProperty("api.customer.service.endpoint", "/api/customer");
        
        // Configure RestAssured
        RestAssured.baseURI = baseUrl;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    protected void loadTestProperties() {
        testProperties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application-test.properties")) {
            if (input != null) {
                testProperties.load(input);
            } else {
                // Fallback to default values if properties file is not found
                testProperties.setProperty("api.base.url", "http://localhost:8081");
                testProperties.setProperty("api.customer.service.endpoint", "/api/customer");
            }
        } catch (IOException e) {
            System.err.println("Error loading test properties: " + e.getMessage());
            // Use default values
            testProperties.setProperty("api.base.url", "http://localhost:8081");
            testProperties.setProperty("api.customer.service.endpoint", "/api/customer");
        }
    }

    protected String getTestProperty(String key, String defaultValue) {
        return testProperties.getProperty(key, defaultValue);
    }

    /**
     * Creates a RestAssured request specification with common headers
     */
    protected RequestSpecification getRequestSpec() {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    /**
     * Creates a customer using RestAssured
     */
    protected Response createCustomerWithRestAssured(CustomerData customerData) {
        return getRequestSpec()
                .body(customerData)
                .when()
                .post(customerServiceEndPoint)
                .then()
                .extract()
                .response();
    }

    /**
     * Updates a customer using RestAssured
     */
    protected Response updateCustomerWithRestAssured(CustomerData customerData) {
        return getRequestSpec()
                .body(customerData)
                .when()
                .put(customerServiceEndPoint + "/" + customerData.getCustomerId())
                .then()
                .extract()
                .response();
    }

    /**
     * Gets a customer using RestAssured
     */
    protected Response getCustomerWithRestAssured(Long customerId) {
        return getRequestSpec()
                .when()
                .get(customerServiceEndPoint + "/" + customerId)
                .then()
                .extract()
                .response();
    }

    /**
     * Deletes a customer using RestAssured
     */
    protected Response deleteCustomerWithRestAssured(Long customerId) {
        return getRequestSpec()
                .when()
                .delete(customerServiceEndPoint + "/" + customerId)
                .then()
                .extract()
                .response();
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
