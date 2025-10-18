package com.example.customer_service_integration_test.customer;

import com.example.customer_service_integration_test.RestAssuredBaseTest;
import com.example.customer_service_integration_test.model.CustomerData;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class CustomerDataServiceNegativeFlowRATest extends RestAssuredBaseTest {

    @BeforeClass
    public void setUp() {
        setupRestAssured();
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

    @DataProvider(name = "invalid_customer_data_json")
    public Object[][] invalidCustomerDataJson() {
        return new Object[][]{
                // Invalid JSON structure - missing required fields
                {"{\"name\":\"testName\"}", "Missing email field"},
                {"{\"email\":\"test@gmail.com\"}", "Missing name field"},
                {"{\"name\":\"testName\",\"email\":\"invalid-email\"}", "Invalid email format"},
                {"{\"name\":\"\",\"email\":\"test@gmail.com\"}", "Empty name field"},
                {"{\"name\":\"testName\",\"email\":\"\"}", "Empty email field"},
                // Malformed JSON
                {"{\"name\":\"testName\",\"email\":\"test@gmail.com\",}", "Trailing comma"},
                {"{name:\"testName\",\"email\":\"test@gmail.com\"}", "Missing quotes around key"},
        };
    }

    @Test(dataProvider = "invalid_customer_data", invocationCount = 1, threadPoolSize = 1)
    public void test_invalid_customer_data_with_restassured(String name, String address, String email, boolean status) {
        // Create customer payload with invalid data
        CustomerData customerData = CustomerData.builder()
                .name(name)
                .address(address)
                .email(email)
                .status(status)
                .build();

        // Make POST request using RestAssured
        Response response = createCustomerWithRestAssured(customerData);

        // Verify the response status using RestAssured assertions
        response.then()
                .statusCode(400)
                .contentType("application/json");

        System.out.println("✅ Correctly received 400 Bad Request for invalid data: " + email);
        System.out.println("Response body: " + response.getBody().asString());

        // Verify that no customer was saved to database with this invalid email
        verifyCustomerNotSavedInDatabase(email);
    }

    @Test(dataProvider = "invalid_customer_data_json", invocationCount = 1, threadPoolSize = 1)
    public void test_invalid_json_payload_with_restassured(String invalidJson, String description) {
        System.out.println("Testing invalid JSON: " + description);

        // Make POST request with invalid JSON using RestAssured
        Response response = given()
                .contentType("application/json")
                .body(invalidJson)
                .when()
                .post(customerServiceEndPoint)
                .then()
                .extract()
                .response();

        // Verify the response status
        response.then()
                .statusCode(anyOf(equalTo(400), equalTo(422))) // Could be 400 or 422 depending on validation
                .contentType("application/json");

        System.out.println("✅ Correctly received error status for invalid JSON: " + description);
        System.out.println("Response body: " + response.getBody().asString());
    }

    @Test(description = "Test creating customer with duplicate email")
    public void test_duplicate_email_with_restassured() {
        // First, create a valid customer
        CustomerData firstCustomer = CustomerData.builder()
                .name("First Customer")
                .address("Stockholm")
                .email("duplicate@test.com")
                .status(true)
                .build();

        Response firstResponse = createCustomerWithRestAssured(firstCustomer);
        firstResponse.then()
                .statusCode(200)
                .contentType("application/json");

        System.out.println("✅ First customer created successfully");

        // Try to create another customer with the same email
        CustomerData duplicateCustomer = CustomerData.builder()
                .name("Second Customer")
                .address("Gothenburg")
                .email("duplicate@test.com") // Same email
                .status(true)
                .build();

        Response duplicateResponse = createCustomerWithRestAssured(duplicateCustomer);
        duplicateResponse.then()
                .statusCode(anyOf(equalTo(400), equalTo(409))) // Could be 400 or 409 depending on implementation
                .contentType("application/json");

        System.out.println("✅ Correctly received error for duplicate email");
        System.out.println("Response body: " + duplicateResponse.getBody().asString());
    }

    @Test(description = "Test creating customer with extremely long name")
    public void test_extremely_long_name_with_restassured() {
        // Create a name that's extremely long (more than typical database field limits)
        String longName = "A".repeat(1000); // 1000 character name

        CustomerData customerData = CustomerData.builder()
                .name(longName)
                .address("Stockholm")
                .email("longname@test.com")
                .status(true)
                .build();

        Response response = createCustomerWithRestAssured(customerData);
        response.then()
                .statusCode(anyOf(equalTo(400), equalTo(422)))
                .contentType("application/json");

        System.out.println("✅ Correctly received error for extremely long name");
        System.out.println("Response body: " + response.getBody().asString());

        // Verify that no customer was saved to database
        verifyCustomerNotSavedInDatabase("longname@test.com");
    }

    @Test(description = "Test creating customer with extremely long email")
    public void test_extremely_long_email_with_restassured() {
        // Create an email that's extremely long
        String longEmail = "a".repeat(250) + "@test.com"; // Very long email

        CustomerData customerData = CustomerData.builder()
                .name("Test Customer")
                .address("Stockholm")
                .email(longEmail)
                .status(true)
                .build();

        Response response = createCustomerWithRestAssured(customerData);
        response.then()
                .statusCode(anyOf(equalTo(400), equalTo(422)))
                .contentType("application/json");

        System.out.println("✅ Correctly received error for extremely long email");
        System.out.println("Response body: " + response.getBody().asString());

        // Verify that no customer was saved to database
        verifyCustomerNotSavedInDatabase(longEmail);
    }

    @Test(description = "Test creating customer with special characters in name")
    public void test_special_characters_in_name_with_restassured() {
        CustomerData customerData = CustomerData.builder()
                .name("Test@#$%^&*()Customer")
                .address("Stockholm")
                .email("specialchars@test.com")
                .status(true)
                .build();

        Response response = createCustomerWithRestAssured(customerData);
        
        // This might be valid or invalid depending on business rules
        // We'll check if it's accepted or rejected
        int statusCode = response.getStatusCode();
        
        if (statusCode == 200) {
            System.out.println("✅ Special characters in name are allowed");
            // If accepted, verify it was saved correctly
            CustomerData savedCustomer = response.as(CustomerData.class);
            verifyCustomerDataSavedInDatabase(savedCustomer.getCustomerId(), customerData);
        } else {
            System.out.println("✅ Special characters in name are rejected with status: " + statusCode);
            System.out.println("Response body: " + response.getBody().asString());
            verifyCustomerNotSavedInDatabase("specialchars@test.com");
        }
    }

    @Test(description = "Test creating customer with SQL injection attempt in name")
    public void test_sql_injection_in_name_with_restassured() {
        CustomerData customerData = CustomerData.builder()
                .name("'; DROP TABLE customers; --")
                .address("Stockholm")
                .email("sqlinjection@test.com")
                .status(true)
                .build();

        Response response = createCustomerWithRestAssured(customerData);
        
        // Should be rejected for security reasons
        response.then()
                .statusCode(anyOf(equalTo(400), equalTo(422)))
                .contentType("application/json");

        System.out.println("✅ SQL injection attempt correctly rejected");
        System.out.println("Response body: " + response.getBody().asString());

        // Verify that no customer was saved to database
        verifyCustomerNotSavedInDatabase("sqlinjection@test.com");
    }

    @Test(description = "Test creating customer with XSS attempt in name")
    public void test_xss_in_name_with_restassured() {
        CustomerData customerData = CustomerData.builder()
                .name("<script>alert('XSS')</script>")
                .address("Stockholm")
                .email("xss@test.com")
                .status(true)
                .build();

        Response response = createCustomerWithRestAssured(customerData);
        
        // This might be accepted but should be sanitized
        int statusCode = response.getStatusCode();
        
        if (statusCode == 200) {
            System.out.println("✅ XSS attempt handled (might be sanitized)");
            CustomerData savedCustomer = response.as(CustomerData.class);
            // Verify the name was sanitized
            Assert.assertNotEquals(savedCustomer.getName(), customerData.getName(), 
                "XSS content should be sanitized");
            verifyCustomerDataSavedInDatabase(savedCustomer.getCustomerId(), savedCustomer);
        } else {
            System.out.println("✅ XSS attempt correctly rejected with status: " + statusCode);
            System.out.println("Response body: " + response.getBody().asString());
            verifyCustomerNotSavedInDatabase("xss@test.com");
        }
    }

    @Test(description = "Test creating customer with null status")
    public void test_null_status_with_restassured() {
        // Create JSON with null status
        String jsonWithNullStatus = "{\"name\":\"Test Customer\",\"address\":\"Stockholm\",\"email\":\"nullstatus@test.com\",\"status\":null}";

        Response response = given()
                .contentType("application/json")
                .body(jsonWithNullStatus)
                .when()
                .post(customerServiceEndPoint)
                .then()
                .extract()
                .response();

        // Should handle null status appropriately
        int statusCode = response.getStatusCode();
        
        if (statusCode == 200) {
            System.out.println("✅ Null status handled (might default to false)");
            CustomerData savedCustomer = response.as(CustomerData.class);
            // Verify status was handled (either defaulted or kept as null)
            System.out.println("Saved customer status: " + savedCustomer.isStatus());
        } else {
            System.out.println("✅ Null status correctly rejected with status: " + statusCode);
            System.out.println("Response body: " + response.getBody().asString());
            verifyCustomerNotSavedInDatabase("nullstatus@test.com");
        }
    }

    @Test(description = "Test creating customer with invalid HTTP method")
    public void test_invalid_http_method_with_restassured() {
        CustomerData customerData = CustomerData.builder()
                .name("Test Customer")
                .address("Stockholm")
                .email("invalidmethod@test.com")
                .status(true)
                .build();

        // Try to use GET method instead of POST for creation
        Response response = given()
                .contentType("application/json")
                .body(customerData)
                .when()
                .get(customerServiceEndPoint)
                .then()
                .extract()
                .response();

        // Should return 405 Method Not Allowed
        response.then()
                .statusCode(405)
                .contentType("application/json");

        System.out.println("✅ Invalid HTTP method correctly rejected");
        System.out.println("Response body: " + response.getBody().asString());
    }

    @Test(description = "Test creating customer with wrong content type")
    public void test_wrong_content_type_with_restassured() {
        CustomerData customerData = CustomerData.builder()
                .name("Test Customer")
                .address("Stockholm")
                .email("wrongcontenttype@test.com")
                .status(true)
                .build();

        // Send with wrong content type
        Response response = given()
                .contentType("text/plain")
                .body(customerData.toString())
                .when()
                .post(customerServiceEndPoint)
                .then()
                .extract()
                .response();

        // Should return 415 Unsupported Media Type or 400 Bad Request
        response.then()
                .statusCode(anyOf(equalTo(400), equalTo(415)))
                .contentType("application/json");

        System.out.println("✅ Wrong content type correctly rejected");
        System.out.println("Response body: " + response.getBody().asString());
    }
}
