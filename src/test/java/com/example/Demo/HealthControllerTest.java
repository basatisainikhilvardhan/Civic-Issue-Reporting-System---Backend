package com.example.Demo;

import com.example.Demo.controller.HealthController;
import com.example.Demo.service.HealthService;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

@SpringBootTest
class HealthControllerTest {

    @Autowired
    private HealthController healthController;

    @Autowired
    private HealthService healthService;

    @Test
    void testHealthEndpointReturnsRunningMessage() {
        ResponseEntity<Map<String, String>> response = healthController.health();
        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("Civic Issue Reporting System is running", response.getBody().get("message"));
    }

    @Test
    void testHealthServiceStatus() {
        Map<String, String> status = healthService.getHealthStatus();
        Assertions.assertNotNull(status);
        Assertions.assertEquals("Civic Issue Reporting System is running", status.get("message"));
    }
}
