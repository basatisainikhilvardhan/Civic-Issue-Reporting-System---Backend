package com.example.Demo.dto;

import com.example.Demo.Role;

public class AuthResponse {

    private String token;
    private String type = "Bearer";
    private Integer id;
    private String name;
    private String email;
    private String phone;
    private Role role;
    private String createdAt;
    private String message;

    public AuthResponse() {
    }

    public AuthResponse(String token, Integer id, String name, String email, String phone, Role role, String createdAt, String message) {
        this.token = token;
        this.type = "Bearer";
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.createdAt = createdAt;
        this.message = message;
    }

    public AuthResponse(String token, Integer id, String name, String email, Role role, String message) {
        this(token, id, name, email, null, role, null, message);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
