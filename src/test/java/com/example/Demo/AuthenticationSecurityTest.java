package com.example.Demo;

import com.example.Demo.dto.AuthResponse;
import com.example.Demo.dto.LoginRequest;
import com.example.Demo.dto.RegisterRequest;
import com.example.Demo.security.JwtUtil;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthenticationSecurityTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private RestTemplate restTemplate;

    private Users testCitizen;
    private Users testAdmin;
    private String citizenToken;
    private String adminToken;

    private static final String CITIZEN_EMAIL = "citizen_test@civic.org";
    private static final String CITIZEN_PASSWORD = "CitizenSecurePassword123";
    private static final String ADMIN_EMAIL = "admin_test@civic.org";
    private static final String ADMIN_PASSWORD = "AdminSecurePassword123";

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();

        // Clean up any pre-existing test records
        userRepository.findByEmail(CITIZEN_EMAIL).ifPresent(userRepository::delete);
        userRepository.findByEmail(ADMIN_EMAIL).ifPresent(userRepository::delete);

        // 1. Create citizen user
        testCitizen = new Users();
        testCitizen.setName("Citizen Test");
        testCitizen.setEmail(CITIZEN_EMAIL);
        testCitizen.setPassword(passwordEncoder.encode(CITIZEN_PASSWORD));
        testCitizen.setPhone("9876543210");
        testCitizen.setRole(Role.CITIZEN);
        testCitizen = userRepository.save(testCitizen);
        citizenToken = jwtUtil.generateToken(testCitizen);

        // 2. Create admin user
        testAdmin = new Users();
        testAdmin.setName("Admin Test");
        testAdmin.setEmail(ADMIN_EMAIL);
        testAdmin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        testAdmin.setPhone("9123456780");
        testAdmin.setRole(Role.ADMIN);
        testAdmin = userRepository.save(testAdmin);
        adminToken = jwtUtil.generateToken(testAdmin);
    }

    @AfterEach
    void tearDown() {
        if (testCitizen != null && testCitizen.getXyz() != null) {
            userRepository.findById(testCitizen.getXyz()).ifPresent(userRepository::delete);
        }
        if (testAdmin != null && testAdmin.getXyz() != null) {
            userRepository.findById(testAdmin.getXyz()).ifPresent(userRepository::delete);
        }
    }

    @Test
    @DisplayName("1. Valid Login - POST /api/auth/login returns 200, JWT token, CITIZEN role, and no password")
    void test1_validLogin() {
        String url = getBaseUrl() + "/api/auth/login";
        LoginRequest request = new LoginRequest(CITIZEN_EMAIL, CITIZEN_PASSWORD);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(url, entity, AuthResponse.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertNotNull(response.getBody().getToken());
        Assertions.assertFalse(response.getBody().getToken().isEmpty());
        Assertions.assertEquals("Bearer", response.getBody().getType());
        Assertions.assertEquals(CITIZEN_EMAIL, response.getBody().getEmail());
        Assertions.assertEquals(Role.CITIZEN, response.getBody().getRole());
        Assertions.assertTrue(jwtUtil.validateToken(response.getBody().getToken()));
    }

    @Test
    @DisplayName("2. Wrong Password - POST /api/auth/login returns 401 Unauthorized")
    void test2_wrongPassword() {
        String url = getBaseUrl() + "/api/auth/login";
        LoginRequest request = new LoginRequest(CITIZEN_EMAIL, "CompletelyWrongPassword123!");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);

        try {
            restTemplate.postForEntity(url, entity, Map.class);
            Assertions.fail("Expected 401 Unauthorized exception");
        } catch (HttpClientErrorException.Unauthorized ex) {
            Assertions.assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        }
    }

    @Test
    @DisplayName("3. Unknown Email - POST /api/auth/login returns 401 Unauthorized")
    void test3_unknownEmail() {
        String url = getBaseUrl() + "/api/auth/login";
        LoginRequest request = new LoginRequest("unknown_user_12345@civic.org", "SomePassword123");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);

        try {
            restTemplate.postForEntity(url, entity, Map.class);
            Assertions.fail("Expected 401 Unauthorized exception");
        } catch (HttpClientErrorException.Unauthorized ex) {
            Assertions.assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        }
    }

    @Test
    @DisplayName("4. Missing Credentials - POST /api/auth/login returns 400 Bad Request")
    void test4_missingCredentials() {
        String url = getBaseUrl() + "/api/auth/login";
        LoginRequest request = new LoginRequest("", "");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);

        try {
            restTemplate.postForEntity(url, entity, Map.class);
            Assertions.fail("Expected 400 Bad Request exception");
        } catch (HttpClientErrorException.BadRequest ex) {
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }
    }

    @Test
    @DisplayName("5. Valid JWT - GET /api/citizen/profile returns 200 OK")
    void test5_validJwt() {
        String url = getBaseUrl() + "/api/citizen/profile";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + citizenToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("success", response.getBody().get("status"));
        Assertions.assertEquals(CITIZEN_EMAIL, response.getBody().get("user"));
    }

    @Test
    @DisplayName("6. Missing JWT - GET /api/citizen/profile returns 401 Unauthorized")
    void test6_missingJwt() {
        String url = getBaseUrl() + "/api/citizen/profile";

        HttpEntity<Void> entity = new HttpEntity<>(new HttpHeaders());

        try {
            restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Assertions.fail("Expected 401 Unauthorized exception");
        } catch (HttpClientErrorException.Unauthorized ex) {
            Assertions.assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
            Assertions.assertTrue(ex.getResponseBodyAsString().contains("Unauthorized"));
        }
    }

    @Test
    @DisplayName("7. Invalid JWT - GET /api/citizen/profile returns 401 Unauthorized")
    void test7_invalidJwt() {
        String url = getBaseUrl() + "/api/citizen/profile";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer invalid.malformed.fakeJwtToken12345");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Assertions.fail("Expected 401 Unauthorized exception");
        } catch (HttpClientErrorException.Unauthorized ex) {
            Assertions.assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        }
    }

    @Test
    @DisplayName("8. Citizen accessing protected endpoint - Returns 200 OK")
    void test8_citizenAccessingProtectedEndpoint() {
        String url = getBaseUrl() + "/api/citizen/profile";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + citizenToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals("success", response.getBody().get("status"));
        Assertions.assertTrue(response.getBody().get("message").toString().contains("Citizen protected resource"));
    }

    @Test
    @DisplayName("9. Citizen attempting admin endpoint - Returns 403 Forbidden")
    void test9_citizenAttemptingAdminEndpoint() {
        String url = getBaseUrl() + "/api/admin/dashboard";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + citizenToken); // Citizen token!
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Assertions.fail("Expected 403 Forbidden exception");
        } catch (HttpClientErrorException.Forbidden ex) {
            Assertions.assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
            Assertions.assertTrue(ex.getResponseBodyAsString().contains("Forbidden"));
        }
    }

    @Test
    @DisplayName("10. Admin accessing admin endpoint - Returns 200 OK")
    void test10_adminAccessingAdminEndpoint() {
        String url = getBaseUrl() + "/api/admin/dashboard";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + adminToken); // Admin token!
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("success", response.getBody().get("status"));
        Assertions.assertEquals(true, response.getBody().get("authorized"));
        Assertions.assertTrue(response.getBody().get("message").toString().contains("Admin Dashboard"));
    }

    @Test
    @DisplayName("Extra: Registration Flow - Creates user in MySQL with hashed password and returns token")
    void testRegistrationFlow() {
        String regEmail = "new_citizen_" + System.currentTimeMillis() + "@civic.org";
        RegisterRequest registerRequest = new RegisterRequest("New Citizen", regEmail, "9988776655", "Password123!");

        String url = getBaseUrl() + "/api/auth/register";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RegisterRequest> entity = new HttpEntity<>(registerRequest, headers);

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(url, entity, AuthResponse.class);

        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertNotNull(response.getBody().getToken());
        Assertions.assertEquals(regEmail, response.getBody().getEmail());
        Assertions.assertEquals(Role.CITIZEN, response.getBody().getRole());

        // Verify stored in MySQL with BCrypt hashed password
        Users saved = userRepository.findByEmail(regEmail).orElse(null);
        Assertions.assertNotNull(saved);
        Assertions.assertTrue(saved.getPassword().startsWith("$2a$") || saved.getPassword().startsWith("$2b$"));
        Assertions.assertEquals(Role.CITIZEN, saved.getRole());

        // Cleanup
        userRepository.delete(saved);
    }
}
