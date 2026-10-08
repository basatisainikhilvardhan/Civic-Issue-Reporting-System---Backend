package com.example.Demo.service;

import com.example.Demo.Role;
import com.example.Demo.UserRepository;
import com.example.Demo.Users;
import com.example.Demo.dto.AuthResponse;
import com.example.Demo.dto.LoginRequest;
import com.example.Demo.dto.RegisterRequest;
import com.example.Demo.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration request cannot be empty");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        String email = request.getEmail().trim().toLowerCase();
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        if (!email.matches(emailRegex)) {
            throw new IllegalArgumentException("Invalid email format");
        }

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (request.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        if (userRepository.existsByEmail(email)) {
            throw new com.example.Demo.exception.DuplicateEmailException("Email is already registered: " + email);
        }

        String name = request.getName().trim();
        if (userRepository.existsByUsername(name)) {
            throw new IllegalArgumentException("Username is already taken: " + name);
        }

        String phone = request.getPhone() != null ? request.getPhone().trim() : null;
        if (phone != null && !phone.isEmpty()) {
            if (!phone.matches("^[0-9+\\-\\s()]{7,20}$")) {
                throw new IllegalArgumentException("Invalid phone number format");
            }
        }

        Users newUser = new Users();
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setPhone(phone);
        // BCrypt password hashing
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        // Always default to CITIZEN role, cannot self-register as ADMIN through public API
        newUser.setRole(Role.CITIZEN);
        newUser.setCreatedAt(java.time.LocalDateTime.now());

        Users savedUser = userRepository.save(newUser);
        String token = jwtUtil.generateToken(savedUser);

        return new AuthResponse(
                token,
                savedUser.getXyz(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getPhone(),
                savedUser.getRole(),
                savedUser.getCreatedAt() != null ? savedUser.getCreatedAt().toString() : null,
                "User registered successfully"
        );
    }

    public AuthResponse login(LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Credentials are required");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty() ||
            request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Missing email or password");
        }

        String email = request.getEmail().trim().toLowerCase();

        Users user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    Users u = userRepository.findByUsername(request.getEmail().trim());
                    if (u == null) {
                        throw new BadCredentialsException("Invalid email or password");
                    }
                    return u;
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user);

        return new AuthResponse(
                token,
                user.getXyz(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                "Login successful"
        );
    }
}
