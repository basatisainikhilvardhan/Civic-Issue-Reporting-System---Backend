package com.example.Demo;

import com.example.Demo.dto.CreateIssueRequest;
import com.example.Demo.dto.IssueResponse;
import com.example.Demo.dto.IssueStatusHistoryResponse;
import com.example.Demo.dto.StatusUpdateResponse;
import com.example.Demo.dto.UpdateStatusRequest;
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
public class IssueStatusManagementTest {

    @LocalServerPort
    private int port;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private IssueStatusHistoryRepository issueStatusHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private RestTemplate restTemplate;

    private Users adminUser;
    private Users citizen1;
    private Users citizen2;

    private String adminToken;
    private String citizen1Token;
    private String citizen2Token;

    private static final String ADMIN_EMAIL = "admin_status_test@civic.gov.in";
    private static final String CITIZEN1_EMAIL = "citizen1_status_test@civic.gov.in";
    private static final String CITIZEN2_EMAIL = "citizen2_status_test@civic.gov.in";

    private String getAdminBaseUrl() {
        return "http://localhost:" + port + "/api/admin/issues";
    }

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

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();

        // Clean up any old histories and issues
        issueStatusHistoryRepository.deleteAll();
        issueRepository.deleteAll();

        userRepository.findByEmail(ADMIN_EMAIL).ifPresent(userRepository::delete);
        userRepository.findByEmail(CITIZEN1_EMAIL).ifPresent(userRepository::delete);
        userRepository.findByEmail(CITIZEN2_EMAIL).ifPresent(userRepository::delete);

        // 1. Create Admin
        adminUser = new Users();
        adminUser.setName("System Admin");
        adminUser.setEmail(ADMIN_EMAIL);
        adminUser.setPassword(passwordEncoder.encode("AdminPass123!"));
        adminUser.setRole(Role.ADMIN);
        adminUser = userRepository.save(adminUser);
        adminToken = jwtUtil.generateToken(adminUser);

        // 2. Create Citizen 1
        citizen1 = new Users();
        citizen1.setName("Citizen One");
        citizen1.setEmail(CITIZEN1_EMAIL);
        citizen1.setPassword(passwordEncoder.encode("CitPass123!"));
        citizen1.setRole(Role.CITIZEN);
        citizen1 = userRepository.save(citizen1);
        citizen1Token = jwtUtil.generateToken(citizen1);

        // 3. Create Citizen 2
        citizen2 = new Users();
        citizen2.setName("Citizen Two");
        citizen2.setEmail(CITIZEN2_EMAIL);
        citizen2.setPassword(passwordEncoder.encode("CitPass123!"));
        citizen2.setRole(Role.CITIZEN);
        citizen2 = userRepository.save(citizen2);
        citizen2Token = jwtUtil.generateToken(citizen2);
    }

    @AfterEach
    void tearDown() {
        issueStatusHistoryRepository.deleteAll();
        issueRepository.deleteAll();

        if (adminUser != null && adminUser.getXyz() != null) {
            userRepository.findById(adminUser.getXyz()).ifPresent(userRepository::delete);
        }
        if (citizen1 != null && citizen1.getXyz() != null) {
            userRepository.findById(citizen1.getXyz()).ifPresent(userRepository::delete);
        }
        if (citizen2 != null && citizen2.getXyz() != null) {
            userRepository.findById(citizen2.getXyz()).ifPresent(userRepository::delete);
        }
    }

    private Integer createTestIssue() {
        CreateIssueRequest req = new CreateIssueRequest(
                "Large Pothole on Ring Road",
                "Deep pothole near junction causing safety hazard",
                "ROAD",
                "Ring Road Junction 4",
                "HIGH",
                "https://example.com/pothole.jpg"
        );
        HttpEntity<CreateIssueRequest> entity = new HttpEntity<>(req, createAuthHeaders(citizen1Token));
        ResponseEntity<IssueResponse> res = restTemplate.postForEntity(getIssuesBaseUrl(), entity, IssueResponse.class);
        return res.getBody().getId();
    }

    @Test
    @DisplayName("1. Status Transition: NEW -> ASSIGNED - Updates status, creates history, stores remarks and admin info")
    void test1_statusTransition_NewToAssigned() {
        Integer issueId = createTestIssue();

        UpdateStatusRequest req = new UpdateStatusRequest("ASSIGNED", "Assigned to Ward 12 Road Maintenance Team");
        HttpEntity<UpdateStatusRequest> entity = new HttpEntity<>(req, createAuthHeaders(adminToken));

        ResponseEntity<StatusUpdateResponse> response = restTemplate.exchange(
                getAdminBaseUrl() + "/" + issueId + "/status",
                HttpMethod.PUT,
                entity,
                StatusUpdateResponse.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(issueId, response.getBody().getId());
        Assertions.assertEquals(IssueStatus.NEW, response.getBody().getOldStatus());
        Assertions.assertEquals(IssueStatus.ASSIGNED, response.getBody().getNewStatus());
        Assertions.assertEquals(IssueStatus.ASSIGNED, response.getBody().getStatus());
        Assertions.assertEquals("Assigned to Ward 12 Road Maintenance Team", response.getBody().getRemarks());
        Assertions.assertEquals(adminUser.getId(), response.getBody().getChangedById());
        Assertions.assertEquals(ADMIN_EMAIL, response.getBody().getChangedByEmail());
        Assertions.assertNotNull(response.getBody().getChangedAt());

        // Verify history list in response
        Assertions.assertEquals(1, response.getBody().getStatusHistory().size());
        IssueStatusHistoryResponse h = response.getBody().getStatusHistory().get(0);
        Assertions.assertEquals(IssueStatus.NEW, h.getOldStatus());
        Assertions.assertEquals(IssueStatus.ASSIGNED, h.getNewStatus());
    }

    @Test
    @DisplayName("2. Status Transition: ASSIGNED -> IN_PROGRESS - Transitions properly with history appended")
    void test2_statusTransition_AssignedToInProgress() {
        Integer issueId = createTestIssue();

        // 1. Move to ASSIGNED
        UpdateStatusRequest req1 = new UpdateStatusRequest("ASSIGNED", "Assigned to Road Team");
        restTemplate.exchange(getAdminBaseUrl() + "/" + issueId + "/status", HttpMethod.PUT, new HttpEntity<>(req1, createAuthHeaders(adminToken)), StatusUpdateResponse.class);

        // 2. Move to IN_PROGRESS
        UpdateStatusRequest req2 = new UpdateStatusRequest("IN_PROGRESS", "Repair crew on site with asphalt mixture");
        ResponseEntity<StatusUpdateResponse> response = restTemplate.exchange(
                getAdminBaseUrl() + "/" + issueId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(req2, createAuthHeaders(adminToken)),
                StatusUpdateResponse.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(IssueStatus.ASSIGNED, response.getBody().getOldStatus());
        Assertions.assertEquals(IssueStatus.IN_PROGRESS, response.getBody().getNewStatus());
        Assertions.assertEquals(IssueStatus.IN_PROGRESS, response.getBody().getStatus());
        Assertions.assertEquals("Repair crew on site with asphalt mixture", response.getBody().getRemarks());

        // Status history has 2 chronological entries
        Assertions.assertEquals(2, response.getBody().getStatusHistory().size());
        Assertions.assertEquals(IssueStatus.NEW, response.getBody().getStatusHistory().get(0).getOldStatus());
        Assertions.assertEquals(IssueStatus.ASSIGNED, response.getBody().getStatusHistory().get(0).getNewStatus());
        Assertions.assertEquals(IssueStatus.ASSIGNED, response.getBody().getStatusHistory().get(1).getOldStatus());
        Assertions.assertEquals(IssueStatus.IN_PROGRESS, response.getBody().getStatusHistory().get(1).getNewStatus());
    }

    @Test
    @DisplayName("3. Status Transition: IN_PROGRESS -> RESOLVED - Completes issue lifecycle with full history")
    void test3_statusTransition_InProgressToResolved() {
        Integer issueId = createTestIssue();

        // NEW -> ASSIGNED
        restTemplate.exchange(getAdminBaseUrl() + "/" + issueId + "/status", HttpMethod.PUT,
                new HttpEntity<>(new UpdateStatusRequest("ASSIGNED", "Assigned"), createAuthHeaders(adminToken)), StatusUpdateResponse.class);

        // ASSIGNED -> IN_PROGRESS
        restTemplate.exchange(getAdminBaseUrl() + "/" + issueId + "/status", HttpMethod.PUT,
                new HttpEntity<>(new UpdateStatusRequest("IN_PROGRESS", "Work started"), createAuthHeaders(adminToken)), StatusUpdateResponse.class);

        // IN_PROGRESS -> RESOLVED
        UpdateStatusRequest req3 = new UpdateStatusRequest("RESOLVED", "Pothole filled and road resurfaced. Inspection passed.");
        ResponseEntity<StatusUpdateResponse> response = restTemplate.exchange(
                getAdminBaseUrl() + "/" + issueId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(req3, createAuthHeaders(adminToken)),
                StatusUpdateResponse.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(IssueStatus.IN_PROGRESS, response.getBody().getOldStatus());
        Assertions.assertEquals(IssueStatus.RESOLVED, response.getBody().getNewStatus());
        Assertions.assertEquals(IssueStatus.RESOLVED, response.getBody().getStatus());
        Assertions.assertEquals("Pothole filled and road resurfaced. Inspection passed.", response.getBody().getRemarks());

        // Full history has 3 entries
        Assertions.assertEquals(3, response.getBody().getStatusHistory().size());
        Assertions.assertEquals(IssueStatus.RESOLVED, response.getBody().getStatusHistory().get(2).getNewStatus());
    }

    @Test
    @DisplayName("4. Status Transition: REJECTED where appropriate - e.g. Duplicate or invalid issue")
    void test4_statusTransition_RejectedWhereAppropriate() {
        Integer issueId = createTestIssue();

        // NEW -> REJECTED
        UpdateStatusRequest req = new UpdateStatusRequest("REJECTED", "Duplicate report: already registered under issue #45");
        ResponseEntity<StatusUpdateResponse> response = restTemplate.exchange(
                getAdminBaseUrl() + "/" + issueId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(req, createAuthHeaders(adminToken)),
                StatusUpdateResponse.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(IssueStatus.NEW, response.getBody().getOldStatus());
        Assertions.assertEquals(IssueStatus.REJECTED, response.getBody().getNewStatus());
        Assertions.assertEquals(IssueStatus.REJECTED, response.getBody().getStatus());
        Assertions.assertEquals("Duplicate report: already registered under issue #45", response.getBody().getRemarks());
    }

    @Test
    @DisplayName("5. Database Verification: Issue status and IssueStatusHistory persisted directly in MySQL")
    void test5_verifyDatabasePersistenceOfStatusAndHistory() {
        Integer issueId = createTestIssue();

        UpdateStatusRequest req = new UpdateStatusRequest("ASSIGNED", "Assigned to Sanitation Department");
        restTemplate.exchange(getAdminBaseUrl() + "/" + issueId + "/status", HttpMethod.PUT,
                new HttpEntity<>(req, createAuthHeaders(adminToken)), StatusUpdateResponse.class);

        // Verify Issue record in MySQL
        Issue issueInDb = issueRepository.findById(issueId).orElse(null);
        Assertions.assertNotNull(issueInDb, "Issue must exist in MySQL");
        Assertions.assertEquals(IssueStatus.ASSIGNED, issueInDb.getStatus(), "Issue status in MySQL must be ASSIGNED");

        // Verify IssueStatusHistory records in MySQL
        List<IssueStatusHistory> histories = issueStatusHistoryRepository.findByIssueIdOrderByIdAsc(issueId);
        Assertions.assertEquals(1, histories.size(), "Exactly 1 status history record must exist in MySQL");

        IssueStatusHistory h = histories.get(0);
        Assertions.assertNotNull(h.getId(), "History ID must be generated");
        Assertions.assertEquals(issueId, h.getIssue().getId());
        Assertions.assertEquals(IssueStatus.NEW, h.getOldStatus(), "Old status in MySQL must be NEW");
        Assertions.assertEquals(IssueStatus.ASSIGNED, h.getNewStatus(), "New status in MySQL must be ASSIGNED");
        Assertions.assertEquals("Assigned to Sanitation Department", h.getRemarks(), "Remarks in MySQL must match");
        Assertions.assertEquals(adminUser.getId(), h.getChangedBy().getId(), "ChangedBy in MySQL must match admin");
        Assertions.assertNotNull(h.getChangedAt(), "Timestamp in MySQL must not be null");
    }

    @Test
    @DisplayName("6. Unauthorized: Unauthenticated request - Returns 401 Unauthorized / 403 Forbidden")
    void test6_unauthorizedUser_NoToken() {
        Integer issueId = createTestIssue();

        UpdateStatusRequest req = new UpdateStatusRequest("ASSIGNED", "Attempt without token");
        HttpEntity<UpdateStatusRequest> entity = new HttpEntity<>(req, createAuthHeaders(null));

        try {
            restTemplate.exchange(getAdminBaseUrl() + "/" + issueId + "/status", HttpMethod.PUT, entity, Map.class);
            Assertions.fail("Expected HTTP 401/403 for unauthenticated request");
        } catch (HttpClientErrorException ex) {
            Assertions.assertTrue(ex.getStatusCode() == HttpStatus.UNAUTHORIZED || ex.getStatusCode() == HttpStatus.FORBIDDEN);
        }
    }

    @Test
    @DisplayName("7. Unauthorized: Citizen token on Admin Endpoint - Returns 403 Forbidden")
    void test7_unauthorizedUser_CitizenToken() {
        Integer issueId = createTestIssue();

        // Citizen attempts to change issue status
        UpdateStatusRequest req = new UpdateStatusRequest("RESOLVED", "Citizen trying to resolve own issue");
        HttpEntity<UpdateStatusRequest> entity = new HttpEntity<>(req, createAuthHeaders(citizen1Token));

        try {
            restTemplate.exchange(getAdminBaseUrl() + "/" + issueId + "/status", HttpMethod.PUT, entity, Map.class);
            Assertions.fail("Expected HTTP 403 Forbidden when citizen calls admin status endpoint");
        } catch (HttpClientErrorException.Forbidden ex) {
            Assertions.assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        }
    }

    @Test
    @DisplayName("8. Invalid Status: Rejects unknown status with 400 Bad Request")
    void test8_invalidStatus_BadRequest() {
        Integer issueId = createTestIssue();

        UpdateStatusRequest req = new UpdateStatusRequest("INVALID_STATUS", "Invalid status attempt");
        HttpEntity<UpdateStatusRequest> entity = new HttpEntity<>(req, createAuthHeaders(adminToken));

        try {
            restTemplate.exchange(getAdminBaseUrl() + "/" + issueId + "/status", HttpMethod.PUT, entity, Map.class);
            Assertions.fail("Expected HTTP 400 Bad Request for invalid status");
        } catch (HttpClientErrorException.BadRequest ex) {
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }
    }

    @Test
    @DisplayName("9. Non-existent Issue ID: Returns 404 Not Found")
    void test9_nonExistentIssue_NotFound() {
        UpdateStatusRequest req = new UpdateStatusRequest("ASSIGNED", "Status for non-existent issue");
        HttpEntity<UpdateStatusRequest> entity = new HttpEntity<>(req, createAuthHeaders(adminToken));

        try {
            restTemplate.exchange(getAdminBaseUrl() + "/999999/status", HttpMethod.PUT, entity, Map.class);
            Assertions.fail("Expected HTTP 404 Not Found for non-existent issue");
        } catch (HttpClientErrorException.NotFound ex) {
            Assertions.assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

    @Test
    @DisplayName("10. Same Status: Rejects transition to same status with 400 Bad Request")
    void test10_sameStatus_BadRequest() {
        Integer issueId = createTestIssue();

        // Issue starts in NEW, admin tries to set NEW again
        UpdateStatusRequest req = new UpdateStatusRequest("NEW", "Setting NEW again");
        HttpEntity<UpdateStatusRequest> entity = new HttpEntity<>(req, createAuthHeaders(adminToken));

        try {
            restTemplate.exchange(getAdminBaseUrl() + "/" + issueId + "/status", HttpMethod.PUT, entity, Map.class);
            Assertions.fail("Expected HTTP 400 Bad Request when updating to same status");
        } catch (HttpClientErrorException.BadRequest ex) {
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }
    }

    @Test
    @DisplayName("11. History Retrieval: Citizen views own issue history")
    void test11_citizenViewsOwnStatusHistory() {
        Integer issueId = createTestIssue();

        // Admin updates status: NEW -> ASSIGNED -> IN_PROGRESS
        restTemplate.exchange(getAdminBaseUrl() + "/" + issueId + "/status", HttpMethod.PUT,
                new HttpEntity<>(new UpdateStatusRequest("ASSIGNED", "Assigned to Ward 10"), createAuthHeaders(adminToken)), StatusUpdateResponse.class);
        restTemplate.exchange(getAdminBaseUrl() + "/" + issueId + "/status", HttpMethod.PUT,
                new HttpEntity<>(new UpdateStatusRequest("IN_PROGRESS", "Crew on site"), createAuthHeaders(adminToken)), StatusUpdateResponse.class);

        // Citizen 1 (the owner) queries GET /api/issues/{id}/history
        ResponseEntity<List<IssueStatusHistoryResponse>> response = restTemplate.exchange(
                getIssuesBaseUrl() + "/" + issueId + "/history",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(citizen1Token)),
                new ParameterizedTypeReference<List<IssueStatusHistoryResponse>>() {}
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(2, response.getBody().size());
        Assertions.assertEquals(IssueStatus.NEW, response.getBody().get(0).getOldStatus());
        Assertions.assertEquals(IssueStatus.ASSIGNED, response.getBody().get(0).getNewStatus());
        Assertions.assertEquals(IssueStatus.ASSIGNED, response.getBody().get(1).getOldStatus());
        Assertions.assertEquals(IssueStatus.IN_PROGRESS, response.getBody().get(1).getNewStatus());
    }

    @Test
    @DisplayName("12. History Security: Other citizen blocked with 403 Forbidden from viewing another citizen's history")
    void test12_otherCitizenCannotViewStatusHistory() {
        Integer issueId = createTestIssue();

        // Citizen 2 attempts to query Citizen 1's issue history
        try {
            restTemplate.exchange(
                    getIssuesBaseUrl() + "/" + issueId + "/history",
                    HttpMethod.GET,
                    new HttpEntity<>(createAuthHeaders(citizen2Token)),
                    Map.class
            );
            Assertions.fail("Expected HTTP 403 Forbidden for unauthorized citizen viewing history");
        } catch (HttpClientErrorException.Forbidden ex) {
            Assertions.assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        }
    }
}
