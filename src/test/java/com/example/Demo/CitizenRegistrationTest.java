package com.example.Demo;

import com.example.Demo.dto.AuthResponse;
import com.example.Demo.dto.RegisterRequest;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CitizenRegistrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private IssueRepository issueRepository;

    @Autowired(required = false)
    private IssueStatusHistoryRepository issueStatusHistoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private RestTemplate restTemplate;

    private static final String TEST_EMAIL_PREFIX = "reg_test_";

    private String getRegisterUrl() {
        return "http://localhost:" + port + "/api/auth/register";
    }

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
    }

    private void deleteUserAndIssues(Users u) {
        if (issueRepository != null) {
            issueRepository.findByReportedByOrderByCreatedAtDesc(u).forEach(issue -> {
                if (issueStatusHistoryRepository != null) {
                    issueStatusHistoryRepository.findByIssueIdOrderByIdAsc(issue.getId())
                            .forEach(issueStatusHistoryRepository::delete);
                }
                issueRepository.delete(issue);
            });
        }
        userRepository.delete(u);
    }

    @AfterEach
    void tearDown() {
        // Clean up test registrations
        userRepository.findAll().forEach(u -> {
            if (u.getEmail() != null && u.getEmail().startsWith(TEST_EMAIL_PREFIX)) {
                deleteUserAndIssues(u);
            }
        });
    }

    @Test
    @DisplayName("1. Valid Registration - Successfully creates citizen and returns 201 Created")
    void test1_validRegistration() {
        String email = TEST_EMAIL_PREFIX + "valid_" + System.currentTimeMillis() + "@civic.org";
        RegisterRequest request = new RegisterRequest("Valid Citizen", email, "9876543210", "StrongPassword123!");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RegisterRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(getRegisterUrl(), entity, AuthResponse.class);

        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertNotNull(response.getBody().getId());
        Assertions.assertTrue(response.getBody().getId() > 0);
        Assertions.assertEquals("Valid Citizen", response.getBody().getName());
        Assertions.assertEquals(email, response.getBody().getEmail());
        Assertions.assertEquals(Role.CITIZEN, response.getBody().getRole());
        Assertions.assertEquals("User registered successfully", response.getBody().getMessage());
    }

    @Test
    @DisplayName("2. Duplicate Email - Returns 409 Conflict when registering already existing email")
    void test2_duplicateEmail() {
        String email = TEST_EMAIL_PREFIX + "dup_" + System.currentTimeMillis() + "@civic.org";
        RegisterRequest first = new RegisterRequest("Citizen One", email, "9876543210", "Password123!");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity(getRegisterUrl(), new HttpEntity<>(first, headers), AuthResponse.class);

        // Attempt duplicate registration
        RegisterRequest duplicate = new RegisterRequest("Citizen Two", email, "9876543211", "AnotherPassword123!");

        try {
            restTemplate.postForEntity(getRegisterUrl(), new HttpEntity<>(duplicate, headers), Map.class);
            Assertions.fail("Expected 409 Conflict exception on duplicate email");
        } catch (HttpClientErrorException.Conflict ex) {
            Assertions.assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
            Assertions.assertTrue(ex.getResponseBodyAsString().contains("Email is already registered"));
        }
    }

    @Test
    @DisplayName("3. Invalid Email - Returns 400 Bad Request for malformed email")
    void test3_invalidEmail() {
        RegisterRequest request = new RegisterRequest("Citizen Invalid", "not-a-valid-email", "9876543210", "Password123!");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            restTemplate.postForEntity(getRegisterUrl(), new HttpEntity<>(request, headers), Map.class);
            Assertions.fail("Expected 400 Bad Request exception for malformed email");
        } catch (HttpClientErrorException.BadRequest ex) {
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            Assertions.assertTrue(ex.getResponseBodyAsString().contains("Invalid email format"));
        }
    }

    @Test
    @DisplayName("4. Missing Name - Returns 400 Bad Request when name is empty or missing")
    void test4_missingName() {
        String email = TEST_EMAIL_PREFIX + "noname_" + System.currentTimeMillis() + "@civic.org";
        RegisterRequest request = new RegisterRequest("", email, "9876543210", "Password123!");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            restTemplate.postForEntity(getRegisterUrl(), new HttpEntity<>(request, headers), Map.class);
            Assertions.fail("Expected 400 Bad Request exception for missing name");
        } catch (HttpClientErrorException.BadRequest ex) {
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            Assertions.assertTrue(ex.getResponseBodyAsString().contains("Name is required"));
        }
    }

    @Test
    @DisplayName("5. Missing Password - Returns 400 Bad Request when password is empty or missing")
    void test5_missingPassword() {
        String email = TEST_EMAIL_PREFIX + "nopass_" + System.currentTimeMillis() + "@civic.org";
        RegisterRequest request = new RegisterRequest("No Pass Citizen", email, "9876543210", "");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            restTemplate.postForEntity(getRegisterUrl(), new HttpEntity<>(request, headers), Map.class);
            Assertions.fail("Expected 400 Bad Request exception for missing password");
        } catch (HttpClientErrorException.BadRequest ex) {
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            Assertions.assertTrue(ex.getResponseBodyAsString().contains("Password is required"));
        }
    }

    @Test
    @DisplayName("6. Invalid Password - Returns 400 Bad Request when password is shorter than 6 characters")
    void test6_invalidPassword() {
        String email = TEST_EMAIL_PREFIX + "shortpass_" + System.currentTimeMillis() + "@civic.org";
        RegisterRequest request = new RegisterRequest("Short Pass", email, "9876543210", "123");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            restTemplate.postForEntity(getRegisterUrl(), new HttpEntity<>(request, headers), Map.class);
            Assertions.fail("Expected 400 Bad Request exception for short password");
        } catch (HttpClientErrorException.BadRequest ex) {
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            Assertions.assertTrue(ex.getResponseBodyAsString().contains("Password must be at least 6 characters"));
        }
    }

    @Test
    @DisplayName("7. Database Persistence - Verifies registration creates and saves record in MySQL database")
    void test7_databasePersistence() {
        String email = TEST_EMAIL_PREFIX + "persist_" + System.currentTimeMillis() + "@civic.org";
        RegisterRequest request = new RegisterRequest("Persist Citizen", email, "9123456789", "SecurePassword456!");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(getRegisterUrl(), new HttpEntity<>(request, headers), AuthResponse.class);

        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());

        // Verify direct from MySQL via UserRepository
        Users saved = userRepository.findByEmail(email).orElse(null);
        Assertions.assertNotNull(saved, "Saved user must exist in MySQL database");
        Assertions.assertEquals("Persist Citizen", saved.getName());
        Assertions.assertEquals(email, saved.getEmail());
        Assertions.assertEquals("9123456789", saved.getPhone());
        Assertions.assertEquals(Role.CITIZEN, saved.getRole());
        Assertions.assertNotNull(saved.getCreatedAt());
    }

    @Test
    @DisplayName("8. Password is Hashed - Verifies stored password in MySQL is BCrypt hashed and never plain text")
    void test8_passwordIsHashed() {
        String plainPassword = "MySecretRawPassword789!";
        String email = TEST_EMAIL_PREFIX + "hash_" + System.currentTimeMillis() + "@civic.org";
        RegisterRequest request = new RegisterRequest("Hash Citizen", email, "9000000000", plainPassword);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity(getRegisterUrl(), new HttpEntity<>(request, headers), AuthResponse.class);

        // Fetch from MySQL
        Users saved = userRepository.findByEmail(email).orElse(null);
        Assertions.assertNotNull(saved);

        // Password MUST NOT be stored as plain text
        Assertions.assertNotEquals(plainPassword, saved.getPassword(), "Plain text password must never be stored in MySQL");

        // Stored password must be a BCrypt hash
        Assertions.assertTrue(
                saved.getPassword().startsWith("$2a$") || saved.getPassword().startsWith("$2b$"),
                "Stored password in MySQL must be BCrypt hashed"
        );

        // Verify BCrypt matches the plain password
        Assertions.assertTrue(
                passwordEncoder.matches(plainPassword, saved.getPassword()),
                "BCrypt encoder must match raw password with stored hash"
        );
    }

    @Test
    @DisplayName("9. Password is not returned - Verifies API response never returns user password")
    void test9_passwordIsNotReturned() {
        String plainPassword = "NoReturnPassword999!";
        String email = TEST_EMAIL_PREFIX + "nopwreturn_" + System.currentTimeMillis() + "@civic.org";
        RegisterRequest request = new RegisterRequest("No Return Citizen", email, "9555555555", plainPassword);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = restTemplate.postForEntity(getRegisterUrl(), new HttpEntity<>(request, headers), Map.class);

        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());

        // Password must NOT be a field in the response map
        Assertions.assertFalse(response.getBody().containsKey("password"), "Response body must not contain password field");
        Assertions.assertNull(response.getBody().get("password"), "Password in response must be null");
    }

    @Test
    @DisplayName("10. Live MySQL Record - Registers a permanent demo citizen for viva and database verification")
    void test10_verifyLiveRegistrationRecordRetainedInMySQL() {
        String email = "citizen.demo@civic.gov.in";
        userRepository.findByEmail(email).ifPresent(this::deleteUserAndIssues);

        RegisterRequest request = new RegisterRequest("Ramesh Kumar", email, "9876543210", "CivicCitizen@2026");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(getRegisterUrl(), new HttpEntity<>(request, headers), AuthResponse.class);

        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(email, response.getBody().getEmail());
        Assertions.assertEquals(Role.CITIZEN, response.getBody().getRole());

        // Verify in MySQL
        Users saved = userRepository.findByEmail(email).orElse(null);
        Assertions.assertNotNull(saved);
        Assertions.assertTrue(saved.getPassword().startsWith("$2a$") || saved.getPassword().startsWith("$2b$"));
    }
}
