package com.example.customer_service_integration_test;

import com.example.customer_service_integration_test.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class BaseTest {
    protected RestTemplate restTemplate;
    protected ObjectMapper objectMapper;
    protected String baseUrl;
    protected String createEndpoint;
    protected Properties testProperties;



    public void setUp() {
        restTemplate = new RestTemplate();
        objectMapper = new ObjectMapper();
        loadTestProperties();
        baseUrl = getTestProperty("api.base.url", "http://localhost:8081");
        createEndpoint = getTestProperty("api.customer.create.endpoint", "/api/customer/create");
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

}
