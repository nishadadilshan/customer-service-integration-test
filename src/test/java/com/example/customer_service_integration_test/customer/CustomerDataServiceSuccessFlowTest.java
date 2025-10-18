package com.example.customer_service_integration_test.customer;

import com.example.customer_service_integration_test.BaseTest;
import com.example.customer_service_integration_test.model.CustomerData;
import net.datafaker.Faker;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class CustomerDataServiceSuccessFlowTest extends BaseTest {
    private Faker faker;

    @BeforeClass
    public void setUp() {
        setup();
        faker = new Faker();
    }

    @DataProvider(name = "customer_data")
    public Object[][] customerData() {
        return new Object[][]{
                {faker.name().firstName(), faker.address().city(), faker.internet().emailAddress(), true},
                {faker.name().firstName(), faker.address().city(), faker.internet().emailAddress(), true},
                {faker.name().firstName(), faker.address().city(), faker.internet().emailAddress(), true},
        };
    }

    @Test(dataProvider = "customer_data", invocationCount = 1, threadPoolSize = 1)
    public void test_create_customer_api(String name, String address, String email, boolean status) {
        try {
            // Create customer payload as JSON string using properties
            CustomerData customerData = CustomerData.builder()
                    .name(name)
                    .address(address)
                    .email(email)
                    .status(status)
                    .build();

            //Create Customer data
            ResponseEntity<CustomerData> response = createCustomer(customerData);

            // Verify response
            Assert.assertEquals(response.getStatusCode(), HttpStatus.OK, "API should return 200 OK");
            Assert.assertNotNull(response.getBody(), "Response body should not be null");

            // Parse response to verify customer was created
            CustomerData savedCustomerData = response.getBody();

            // Verify the values match what we sent
            Assert.assertEquals(savedCustomerData.getName(), customerData.getName(), "Customer name should match");
            Assert.assertEquals(savedCustomerData.getEmail(), customerData.getEmail(), "Customer email should match");
            Assert.assertEquals(savedCustomerData.getAddress(), customerData.getAddress(), "Customer address should match");
            Assert.assertEquals(savedCustomerData.isStatus(), customerData.isStatus(), "Customer status should be true");

            // Verify customerId was auto-generated (not 0)
            long customerId = savedCustomerData.getCustomerId();
            Assert.assertNotEquals(customerId, 0, "Customer ID should be auto-generated");

            System.out.println("Customer created successfully with ID: " + customerId);
            System.out.println("Response: " + response.getBody());

            verifyCustomerDataSavedInDatabase(customerId, customerData);
        } catch (Exception e) {
            Assert.fail("Test failed with exception:--->> " + e.getMessage(), e);
        }
    }

    @Test(dataProvider = "customer_data", invocationCount = 1, threadPoolSize = 1)
    public void test_update_customer_data(String name, String address, String email, boolean status) {
        CustomerData customerData = CustomerData.builder()
                .name(name)
                .address(address)
                .email(email)
                .status(status)
                .build();

        //Create Customer data
        ResponseEntity<CustomerData> response = createCustomer(customerData);

        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK, "API Should return 200");
        Assert.assertNotNull(response.getBody(), " Response body should not be null");

        CustomerData savedCustomerData = response.getBody();

        // Verify the values match what we sent
        Assert.assertEquals(savedCustomerData.getName(), customerData.getName(), "Customer name should match");
        Assert.assertEquals(savedCustomerData.getEmail(), customerData.getEmail(), "Customer email should match");
        Assert.assertEquals(savedCustomerData.getAddress(), customerData.getAddress(), "Customer address should match");
        Assert.assertEquals(savedCustomerData.isStatus(), customerData.isStatus(), "Customer status should be true");

        // Verify customerId was auto-generated (not 0)
        long customerId = savedCustomerData.getCustomerId();
        Assert.assertNotEquals(customerId, 0, "Customer ID should be auto-generated");
        System.out.println("Customer created successfully with ID: " + customerId);
        System.out.println("Response: " + response.getBody());
        verifyCustomerDataSavedInDatabase(customerId, customerData);


        //Update the customer data
        savedCustomerData.setName(savedCustomerData.getName() + " Updated");
        updateCustomer(savedCustomerData);

        //Load customer from API
        ResponseEntity<CustomerData> updatedCustomerResponse = getCustomer(savedCustomerData);
        CustomerData updatedCustomerData = updatedCustomerResponse.getBody();

        Assert.assertEquals(updatedCustomerData.getName(), savedCustomerData.getName(), "Customer name should match");
        Assert.assertEquals(updatedCustomerData.getEmail(), savedCustomerData.getEmail(), "Customer email should match");
        Assert.assertEquals(updatedCustomerData.getAddress(), savedCustomerData.getAddress(), "Customer address should match");
        Assert.assertEquals(updatedCustomerData.isStatus(), savedCustomerData.isStatus(), "Customer status should be true");

        //Verify again after update the Name
        verifyCustomerDataSavedInDatabase(customerId, savedCustomerData);
    }
}
