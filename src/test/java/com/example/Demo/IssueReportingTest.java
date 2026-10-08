package com.example.Demo;

import com.example.Demo.dto.CreateIssueRequest;
import com.example.Demo.dto.IssueResponse;
import com.example.Demo.dto.UpdateIssueRequest;
import com.example.Demo.security.JwtUtil;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
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
public class IssueReportingTest {

    @LocalServerPort
    private int port;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired(required = false)
    private IssueStatusHistoryRepository issueStatusHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private RestTemplate restTemplate;

    private Users citizen1;
    private Users citizen2;
    private String citizen1Token;
    private String citizen2Token;

    private static final String CITIZEN1_EMAIL = "issue_citizen1@civic.org";
    private static final String CITIZEN2_EMAIL = "issue_citizen2@civic.org";

    private String getIssuesBaseUrl() {
        return "http://localhost:" + port + "/api/issues";
    }

    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.set("Authorization", "Bearer " + token);
        }
        return headers;
    }

    private void cleanUpIssuesAndHistory() {
        if (issueStatusHistoryRepository != null) {
            issueStatusHistoryRepository.deleteAll();
        }
        issueRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();

        // Clean up test issues first
        cleanUpIssuesAndHistory();

        // Clean up test users
        userRepository.findByEmail(CITIZEN1_EMAIL).ifPresent(userRepository::delete);
        userRepository.findByEmail(CITIZEN2_EMAIL).ifPresent(userRepository::delete);

        // Create Citizen 1
        citizen1 = new Users();
        citizen1.setName("Citizen One");
        citizen1.setEmail(CITIZEN1_EMAIL);
        citizen1.setPassword(passwordEncoder.encode("Password123!"));
        citizen1.setRole(Role.CITIZEN);
        citizen1 = userRepository.save(citizen1);
        citizen1Token = jwtUtil.generateToken(citizen1);

        // Create Citizen 2
        citizen2 = new Users();
        citizen2.setName("Citizen Two");
        citizen2.setEmail(CITIZEN2_EMAIL);
        citizen2.setPassword(passwordEncoder.encode("Password123!"));
        citizen2.setRole(Role.CITIZEN);
        citizen2 = userRepository.save(citizen2);
        citizen2Token = jwtUtil.generateToken(citizen2);
    }

    @AfterEach
    void tearDown() {
        cleanUpIssuesAndHistory();
        if (citizen1 != null && citizen1.getXyz() != null) {
            userRepository.findById(citizen1.getXyz()).ifPresent(userRepository::delete);
        }
        if (citizen2 != null && citizen2.getXyz() != null) {
            userRepository.findById(citizen2.getXyz()).ifPresent(userRepository::delete);
        }
    }

    @Test
    @DisplayName("1. Valid Issue Creation - Returns 201 Created and starts with status NEW")
    void test1_validIssueCreation() {
        CreateIssueRequest request = new CreateIssueRequest(
                "Pothole on 5th Avenue",
                "Deep pothole causing serious vehicle damage near bakery",
                "ROAD",
                "5th Avenue, Ward 12",
                "HIGH",
                "https://example.com/images/pothole.jpg"
        );

        HttpEntity<CreateIssueRequest> entity = new HttpEntity<>(request, createAuthHeaders(citizen1Token));
        ResponseEntity<IssueResponse> response = restTemplate.postForEntity(getIssuesBaseUrl(), entity, IssueResponse.class);

        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertNotNull(response.getBody().getId());
        Assertions.assertTrue(response.getBody().getId() > 0);
        Assertions.assertEquals("Pothole on 5th Avenue", response.getBody().getTitle());
        Assertions.assertEquals(IssueCategory.ROAD, response.getBody().getCategory());
        Assertions.assertEquals(IssuePriority.HIGH, response.getBody().getPriority());
        // CRITICAL REQUIREMENT: New issues must start as NEW
        Assertions.assertEquals(IssueStatus.NEW, response.getBody().getStatus());
        Assertions.assertEquals(CITIZEN1_EMAIL, response.getBody().getReportedByEmail());
    }

    @Test
    @DisplayName("2. Missing Title - Returns 400 Bad Request")
    void test2_missingTitle() {
        CreateIssueRequest request = new CreateIssueRequest(
                "",
                "Valid description for road issue",
                "ROAD",
                "Location A",
                "MEDIUM",
                null
        );

        HttpEntity<CreateIssueRequest> entity = new HttpEntity<>(request, createAuthHeaders(citizen1Token));

        try {
            restTemplate.postForEntity(getIssuesBaseUrl(), entity, Map.class);
            Assertions.fail("Expected 400 Bad Request for missing title");
        } catch (HttpClientErrorException.BadRequest ex) {
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            Assertions.assertTrue(ex.getResponseBodyAsString().contains("Title is required"));
        }
    }

    @Test
    @DisplayName("3. Missing Description - Returns 400 Bad Request")
    void test3_missingDescription() {
        CreateIssueRequest request = new CreateIssueRequest(
                "Valid Title",
                "",
                "WATER",
                "Location B",
                "LOW",
                null
        );

        HttpEntity<CreateIssueRequest> entity = new HttpEntity<>(request, createAuthHeaders(citizen1Token));

        try {
            restTemplate.postForEntity(getIssuesBaseUrl(), entity, Map.class);
            Assertions.fail("Expected 400 Bad Request for missing description");
        } catch (HttpClientErrorException.BadRequest ex) {
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            Assertions.assertTrue(ex.getResponseBodyAsString().contains("Description is required"));
        }
    }

    @Test
    @DisplayName("4. Invalid Category - Returns 400 Bad Request for uncontrolled category")
    void test4_invalidCategory() {
        CreateIssueRequest request = new CreateIssueRequest(
                "Valid Title",
                "Valid Description with sufficient text",
                "INVALID_CATEGORY_NAME",
                "Location C",
                "MEDIUM",
                null
        );

        HttpEntity<CreateIssueRequest> entity = new HttpEntity<>(request, createAuthHeaders(citizen1Token));

        try {
            restTemplate.postForEntity(getIssuesBaseUrl(), entity, Map.class);
            Assertions.fail("Expected 400 Bad Request for invalid category");
        } catch (HttpClientErrorException.BadRequest ex) {
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            Assertions.assertTrue(ex.getResponseBodyAsString().contains("Invalid category"));
        }
    }

    @Test
    @DisplayName("5. Invalid Priority - Returns 400 Bad Request for uncontrolled priority")
    void test5_invalidPriority() {
        CreateIssueRequest request = new CreateIssueRequest(
                "Valid Title",
                "Valid Description with sufficient text",
                "GARBAGE",
                "Location D",
                "SUPER_CRITICAL_INVALID",
                null
        );

        HttpEntity<CreateIssueRequest> entity = new HttpEntity<>(request, createAuthHeaders(citizen1Token));

        try {
            restTemplate.postForEntity(getIssuesBaseUrl(), entity, Map.class);
            Assertions.fail("Expected 400 Bad Request for invalid priority");
        } catch (HttpClientErrorException.BadRequest ex) {
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            Assertions.assertTrue(ex.getResponseBodyAsString().contains("Invalid priority"));
        }
    }

    @Test
    @DisplayName("6. Unauthorized Request - Returns 401 Unauthorized without token")
    void test6_unauthorizedRequest() {
        CreateIssueRequest request = new CreateIssueRequest(
                "Valid Title",
                "Valid Description with sufficient text",
                "STREET_LIGHT",
                "Location E",
                "LOW",
                null
        );

        HttpEntity<CreateIssueRequest> entity = new HttpEntity<>(request, createAuthHeaders(null)); // No token!

        try {
            restTemplate.postForEntity(getIssuesBaseUrl(), entity, Map.class);
            Assertions.fail("Expected 401 Unauthorized without authentication token");
        } catch (HttpClientErrorException.Unauthorized ex) {
            Assertions.assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        }
    }

    @Test
    @DisplayName("7. Citizen creates issue - Successfully linked to the authenticated citizen")
    void test7_citizenCreatesIssue() {
        CreateIssueRequest request = new CreateIssueRequest(
                "Broken Street Lamp",
                "Street lamp pole #42 is flickering and dark at night",
                "STREET_LIGHT",
                "Green Park Main Road",
                "MEDIUM",
                null
        );

        HttpEntity<CreateIssueRequest> entity = new HttpEntity<>(request, createAuthHeaders(citizen1Token));
        ResponseEntity<IssueResponse> response = restTemplate.postForEntity(getIssuesBaseUrl(), entity, IssueResponse.class);

        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Assertions.assertEquals(citizen1.getId(), response.getBody().getReportedById());
        Assertions.assertEquals(CITIZEN1_EMAIL, response.getBody().getReportedByEmail());
        Assertions.assertEquals(IssueStatus.NEW, response.getBody().getStatus());
    }

    @Test
    @DisplayName("8. Citizen retrieves own issues - GET /api/issues/my returns only user's issues")
    void test8_citizenRetrievesOwnIssues() {
        // Citizen 1 creates 2 issues
        CreateIssueRequest req1 = new CreateIssueRequest("Issue 1", "Desc 1", "DRAINAGE", "Loc 1", "LOW", null);
        CreateIssueRequest req2 = new CreateIssueRequest("Issue 2", "Desc 2", "WATER", "Loc 2", "HIGH", null);
        restTemplate.postForEntity(getIssuesBaseUrl(), new HttpEntity<>(req1, createAuthHeaders(citizen1Token)), IssueResponse.class);
        restTemplate.postForEntity(getIssuesBaseUrl(), new HttpEntity<>(req2, createAuthHeaders(citizen1Token)), IssueResponse.class);

        // Citizen 2 creates 1 issue
        CreateIssueRequest req3 = new CreateIssueRequest("Issue 3", "Desc 3", "TRAFFIC", "Loc 3", "MEDIUM", null);
        restTemplate.postForEntity(getIssuesBaseUrl(), new HttpEntity<>(req3, createAuthHeaders(citizen2Token)), IssueResponse.class);

        // Citizen 1 queries /api/issues/my
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(citizen1Token));
        ResponseEntity<List<IssueResponse>> response = restTemplate.exchange(
                getIssuesBaseUrl() + "/my",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<IssueResponse>>() {}
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(2, response.getBody().size());
        for (IssueResponse ir : response.getBody()) {
            Assertions.assertEquals(CITIZEN1_EMAIL, ir.getReportedByEmail());
        }
    }

    @Test
    @DisplayName("9. Citizen retrieves issue details - GET /api/issues/{id} allows owner, rejects other citizen with 403")
    void test9_citizenRetrievesIssueDetails() {
        // Citizen 1 creates issue
        CreateIssueRequest req = new CreateIssueRequest("Garbage Pile", "Huge uncollected garbage", "GARBAGE", "Market Square", "CRITICAL", null);
        ResponseEntity<IssueResponse> created = restTemplate.postForEntity(getIssuesBaseUrl(), new HttpEntity<>(req, createAuthHeaders(citizen1Token)), IssueResponse.class);
        Integer issueId = created.getBody().getId();

        // 1. Citizen 1 (owner) can retrieve details
        HttpEntity<Void> ownerEntity = new HttpEntity<>(createAuthHeaders(citizen1Token));
        ResponseEntity<IssueResponse> ownerResponse = restTemplate.exchange(
                getIssuesBaseUrl() + "/" + issueId,
                HttpMethod.GET,
                ownerEntity,
                IssueResponse.class
        );
        Assertions.assertEquals(HttpStatus.OK, ownerResponse.getStatusCode());
        Assertions.assertEquals("Garbage Pile", ownerResponse.getBody().getTitle());

        // 2. Citizen 2 (different citizen) attempting to access Citizen 1's issue gets 403 Forbidden!
        HttpEntity<Void> otherEntity = new HttpEntity<>(createAuthHeaders(citizen2Token));
        try {
            restTemplate.exchange(
                    getIssuesBaseUrl() + "/" + issueId,
                    HttpMethod.GET,
                    otherEntity,
                    Map.class
            );
            Assertions.fail("Expected 403 Forbidden when accessing another citizen's issue");
        } catch (HttpClientErrorException.Forbidden ex) {
            Assertions.assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        }
    }

    @Test
    @DisplayName("10. Database Persistence - Verifies issue record in MySQL database via JPA repository")
    void test10_databasePersistence() {
        CreateIssueRequest req = new CreateIssueRequest(
                "Water Leakage in Pipeline",
                "Fresh drinking water leaking onto street for 2 days",
                "WATER",
                "Sector 7, Block B",
                "HIGH",
                null
        );

        ResponseEntity<IssueResponse> response = restTemplate.postForEntity(getIssuesBaseUrl(), new HttpEntity<>(req, createAuthHeaders(citizen1Token)), IssueResponse.class);
        Integer issueId = response.getBody().getId();

        // Verify directly in MySQL via IssueRepository
        Issue saved = issueRepository.findById(issueId).orElse(null);
        Assertions.assertNotNull(saved, "Issue must exist in MySQL database");
        Assertions.assertEquals("Water Leakage in Pipeline", saved.getTitle());
        Assertions.assertEquals(IssueCategory.WATER, saved.getCategory());
        Assertions.assertEquals(IssuePriority.HIGH, saved.getPriority());
        Assertions.assertEquals(IssueStatus.NEW, saved.getStatus());
        Assertions.assertEquals(citizen1.getId(), saved.getReportedBy().getId());
    }

    @Test
    @DisplayName("11. Update where permitted - PUT /api/issues/{id} allows owner to update issue in NEW status")
    void test11_updateWherePermitted() {
        CreateIssueRequest req = new CreateIssueRequest(
                "Broken Bench",
                "Park bench is broken",
                "PUBLIC_PROPERTY",
                "City Park",
                "LOW",
                null
        );
        ResponseEntity<IssueResponse> created = restTemplate.postForEntity(getIssuesBaseUrl(), new HttpEntity<>(req, createAuthHeaders(citizen1Token)), IssueResponse.class);
        Integer issueId = created.getBody().getId();

        // Update issue
        UpdateIssueRequest updateReq = new UpdateIssueRequest(
                "Broken Concrete Bench with Sharp Edges",
                "Updated description: concrete bench has sharp rebar exposed",
                "PUBLIC_PROPERTY",
                "City Park North Gate",
                "HIGH",
                "https://example.com/bench_updated.jpg"
        );

        HttpEntity<UpdateIssueRequest> updateEntity = new HttpEntity<>(updateReq, createAuthHeaders(citizen1Token));
        ResponseEntity<IssueResponse> updatedResponse = restTemplate.exchange(
                getIssuesBaseUrl() + "/" + issueId,
                HttpMethod.PUT,
                updateEntity,
                IssueResponse.class
        );

        Assertions.assertEquals(HttpStatus.OK, updatedResponse.getStatusCode());
        Assertions.assertEquals("Broken Concrete Bench with Sharp Edges", updatedResponse.getBody().getTitle());
        Assertions.assertEquals(IssuePriority.HIGH, updatedResponse.getBody().getPriority());
        Assertions.assertEquals("https://example.com/bench_updated.jpg", updatedResponse.getBody().getImageUrl());

        // Verify in MySQL
        Issue saved = issueRepository.findById(issueId).orElse(null);
        Assertions.assertNotNull(saved);
        Assertions.assertEquals("Broken Concrete Bench with Sharp Edges", saved.getTitle());
    }

    @Test
    @DisplayName("12. Delete where permitted - DELETE /api/issues/{id} allows owner to delete issue in NEW status")
    void test12_deleteWherePermitted() {
        CreateIssueRequest req = new CreateIssueRequest(
                "Temporary Test Issue to Delete",
                "This issue will be deleted by the owner",
                "OTHER",
                "Sample Lane",
                "LOW",
                null
        );
        ResponseEntity<IssueResponse> created = restTemplate.postForEntity(getIssuesBaseUrl(), new HttpEntity<>(req, createAuthHeaders(citizen1Token)), IssueResponse.class);
        Integer issueId = created.getBody().getId();

        // Delete issue
        HttpEntity<Void> deleteEntity = new HttpEntity<>(createAuthHeaders(citizen1Token));
        ResponseEntity<Map> deleteResponse = restTemplate.exchange(
                getIssuesBaseUrl() + "/" + issueId,
                HttpMethod.DELETE,
                deleteEntity,
                Map.class
        );

        Assertions.assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
        Assertions.assertEquals(true, deleteResponse.getBody().get("success"));

        // Verify deleted from MySQL
        Assertions.assertFalse(issueRepository.findById(issueId).isPresent(), "Issue should be deleted from MySQL database");

        // Verify subsequent GET returns 404 Not Found
        try {
            restTemplate.exchange(
                    getIssuesBaseUrl() + "/" + issueId,
                    HttpMethod.GET,
                    new HttpEntity<>(createAuthHeaders(citizen1Token)),
                    Map.class
            );
            Assertions.fail("Expected 404 Not Found for deleted issue");
        } catch (HttpClientErrorException.NotFound ex) {
            Assertions.assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }
}
