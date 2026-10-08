package com.example.Demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    @GetMapping("/api/chat")
    public String chat(@RequestParam(value = "message", defaultValue = "Hello") String message) {
        return "Civic Assistant Response: " + message;
    }
}