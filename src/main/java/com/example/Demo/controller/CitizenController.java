package com.example.Demo.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/citizen")
@CrossOrigin(origins = "http://localhost:3000")
public class CitizenController {

    @GetMapping("/profile")
    public ResponseEntity<?> getCitizenProfile(Principal principal) {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "success");
        data.put("user", principal != null ? principal.getName() : "anonymous");
        data.put("message", "Citizen protected resource accessed successfully");
        return ResponseEntity.ok(data);
    }
}
