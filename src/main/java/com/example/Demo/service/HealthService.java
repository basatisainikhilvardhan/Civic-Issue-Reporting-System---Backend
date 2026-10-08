package com.example.Demo.service;

import java.util.Collections;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public Map<String, String> getHealthStatus() {
        return Collections.singletonMap("message", "Civic Issue Reporting System is running");
    }
}
