package com.example.Demo;

import com.example.Demo.dto.CreateIssueRequest;
import com.example.Demo.IssueStatus;
import com.example.Demo.dto.DashboardStatsResponse;
import com.example.Demo.dto.IssueResponse;
import com.example.Demo.dto.IssueStatusHistoryResponse;
import com.example.Demo.dto.LoginRequest;
import com.example.Demo.dto.RegisterRequest;
import com.example.Demo.dto.StatusUpdateResponse;
import com.example.Demo.dto.UpdateStatusRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MasterEndToEndVerificationTest {

    @LocalServerPort
    private int port;

    private RestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private IssueStatusHistoryRepository historyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String baseUrl;
    private String adminEmail = "e2e.admin@example.com";
    private String adminPassword = "AdminPassword@123";

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new org.springframework.web.client.DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws java.io.IOException {
                return false;
            }
        });
        cleanDb();

        // Seed verified Admin user
        Users admin = new Users();
        admin.setUsername("E2E_Admin");
        admin.setEmail(adminEmail);
        admin.setPhone("9876543210");
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
    }

    @AfterEach
    void tearDown() {
        cleanDb();
    }

    private void cleanDb() {
        historyRepository.deleteAll();
        issueRepository.deleteAll();
        userRepository.deleteAll();
    }

    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    @Test
    @DisplayName("Complete 27-Step Citizen & Admin E2E Lifecycle Flow against Real MySQL")
    void testComplete27StepCitizenAndAdminLifecycleFlow() {
        String citizenEmail = "rajesh.citizen@example.com";
        String citizenPassword = "SecurePassword@123";

        // ==========================================
        // CITIZEN PHASE
        // ==========================================

        // 1. Register citizen
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setName("Rajesh Citizen");
        registerReq.setEmail(citizenEmail);
        registerReq.setPhone("9876500001");
        registerReq.setPassword(citizenPassword);

        ResponseEntity<Map> registerRes = restTemplate.postForEntity(
                baseUrl + "/api/auth/register",
                new HttpEntity<>(registerReq),
                Map.class
        );
        assertEquals(HttpStatus.CREATED, registerRes.getStatusCode(), "Step 1: Registration failed");

        // 2. Verify user is saved in MySQL
        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?",
                Integer.class,
                citizenEmail
        );
        assertEquals(1, userCount, "Step 2: Citizen record not found in MySQL");

        String dbRole = jdbcTemplate.queryForObject(
                "SELECT role FROM users WHERE email = ?",
                String.class,
                citizenEmail
        );
        assertEquals("CITIZEN", dbRole, "Step 2: Registered user must default to CITIZEN");

        // 3. Login
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail(citizenEmail);
        loginReq.setPassword(citizenPassword);

        ResponseEntity<Map> loginRes = restTemplate.postForEntity(
                baseUrl + "/api/auth/login",
                new HttpEntity<>(loginReq),
                Map.class
        );
        assertEquals(HttpStatus.OK, loginRes.getStatusCode(), "Step 3: Login failed");

        // 4. Verify authentication response and JWT
        Map<String, Object> loginBody = loginRes.getBody();
        assertNotNull(loginBody, "Login response body must not be null");
        String citizenToken = (String) loginBody.get("token");
        assertNotNull(citizenToken, "Step 4: JWT token missing in login response");
        assertEquals("CITIZEN", loginBody.get("role"), "Step 4: Role mismatch in auth response");

        // 5. Open citizen dashboard (My Reports)
        ResponseEntity<List<IssueResponse>> dashboardRes = restTemplate.exchange(
                baseUrl + "/api/issues/my",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(citizenToken)),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, dashboardRes.getStatusCode(), "Step 5: Citizen dashboard request failed");
        assertEquals(0, dashboardRes.getBody().size(), "Step 5: Initial reports must be 0");

        // 6. Report a civic issue
        CreateIssueRequest createReq = new CreateIssueRequest();
        createReq.setTitle("Severe Pothole causing traffic accident hazard");
        createReq.setCategory("ROAD");
        createReq.setLocation("Near Railway Station Gate 3, Station Road");
        createReq.setPriority("HIGH");
        createReq.setDescription("Extremely deep crater on left lane causing vehicle damage and two-wheeler falls.");
        createReq.setImageUrl("https://example.com/photos/station_road_pothole.jpg");

        ResponseEntity<IssueResponse> reportRes = restTemplate.postForEntity(
                baseUrl + "/api/issues",
                new HttpEntity<>(createReq, createAuthHeaders(citizenToken)),
                IssueResponse.class
        );
        assertEquals(HttpStatus.CREATED, reportRes.getStatusCode(), "Step 6: Issue creation failed");
        IssueResponse createdIssue = reportRes.getBody();
        assertNotNull(createdIssue);
        assertNotNull(createdIssue.getId(), "Issue ID must be assigned");
        Integer issueId = createdIssue.getId();

        // 7. Verify issue is saved in MySQL
        Integer issueDbCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM issues WHERE id = ?",
                Integer.class,
                issueId
        );
        assertEquals(1, issueDbCount, "Step 7: Issue record not persisted in MySQL");

        Map<String, Object> issueRow = jdbcTemplate.queryForMap(
                "SELECT title, category, priority, status FROM issues WHERE id = ?",
                issueId
        );
        assertEquals("Severe Pothole causing traffic accident hazard", issueRow.get("title"));
        assertEquals("ROAD", issueRow.get("category"));
        assertEquals("HIGH", issueRow.get("priority"));
        assertEquals("NEW", issueRow.get("status"));

        // 8. View My Reports
        ResponseEntity<List<IssueResponse>> myReportsRes = restTemplate.exchange(
                baseUrl + "/api/issues/my",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(citizenToken)),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, myReportsRes.getStatusCode());
        assertEquals(1, myReportsRes.getBody().size(), "Step 8: Citizen reports list must contain 1 issue");
        assertEquals(issueId, myReportsRes.getBody().get(0).getId());

        // 9. Open issue details
        ResponseEntity<IssueResponse> detailsRes = restTemplate.exchange(
                baseUrl + "/api/issues/" + issueId,
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(citizenToken)),
                IssueResponse.class
        );
        assertEquals(HttpStatus.OK, detailsRes.getStatusCode(), "Step 9: Failed to open issue details");

        // 10. Verify status is NEW
        assertEquals(IssueStatus.NEW, detailsRes.getBody().getStatus(), "Step 10: Initial status must be NEW");

        // 11. Logout (discarding citizen token)
        citizenToken = null;

        // ==========================================
        // ADMIN PHASE
        // ==========================================

        // 12. Login as admin
        LoginRequest adminLoginReq = new LoginRequest();
        adminLoginReq.setEmail(adminEmail);
        adminLoginReq.setPassword(adminPassword);

        ResponseEntity<Map> adminLoginRes = restTemplate.postForEntity(
                baseUrl + "/api/auth/login",
                new HttpEntity<>(adminLoginReq),
                Map.class
        );
        assertEquals(HttpStatus.OK, adminLoginRes.getStatusCode(), "Step 12: Admin login failed");
        String adminToken = (String) adminLoginRes.getBody().get("token");
        assertNotNull(adminToken, "Admin JWT token required");
        assertEquals("ADMIN", adminLoginRes.getBody().get("role"));

        // 13. Open admin dashboard
        ResponseEntity<DashboardStatsResponse> adminDashRes = restTemplate.exchange(
                baseUrl + "/api/admin/dashboard",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(adminToken)),
                DashboardStatsResponse.class
        );
        assertEquals(HttpStatus.OK, adminDashRes.getStatusCode(), "Step 13: Admin dashboard request failed");

        // 14. Verify dashboard counts against MySQL
        DashboardStatsResponse stats = adminDashRes.getBody();
        assertNotNull(stats);
        assertEquals(1, stats.getTotalIssues(), "Step 14: Total issues count mismatch");
        assertEquals(1, stats.getNewIssues(), "Step 14: NEW issues count mismatch");
        assertEquals(0, stats.getAssignedIssues());
        assertEquals(0, stats.getInProgressIssues());
        assertEquals(0, stats.getResolvedIssues());
        assertEquals(0, stats.getRejectedIssues());

        // 15. View all issues as admin
        ResponseEntity<List<IssueResponse>> allIssuesRes = restTemplate.exchange(
                baseUrl + "/api/admin/issues",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(adminToken)),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, allIssuesRes.getStatusCode(), "Step 15: Admin view all issues failed");
        assertTrue(allIssuesRes.getBody().stream().anyMatch(i -> i.getId().equals(issueId)), "Step 15: Issue missing from admin list");

        // 16. Open the citizen's issue as admin
        ResponseEntity<IssueResponse> adminIssueDetails = restTemplate.exchange(
                baseUrl + "/api/admin/issues/" + issueId,
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(adminToken)),
                IssueResponse.class
        );
        assertEquals(HttpStatus.OK, adminIssueDetails.getStatusCode(), "Step 16: Admin view issue details failed");

        // 17. Change status to ASSIGNED & 18. Add remarks
        UpdateStatusRequest assignReq = new UpdateStatusRequest("ASSIGNED", "Assigned to Ward 4 Public Works Engineering Division");
        ResponseEntity<StatusUpdateResponse> assignRes = restTemplate.exchange(
                baseUrl + "/api/admin/issues/" + issueId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(assignReq, createAuthHeaders(adminToken)),
                StatusUpdateResponse.class
        );
        assertEquals(HttpStatus.OK, assignRes.getStatusCode(), "Step 17: Status update to ASSIGNED failed");

        // 19. Verify database update in MySQL
        String statusAfterAssign = jdbcTemplate.queryForObject(
                "SELECT status FROM issues WHERE id = ?",
                String.class,
                issueId
        );
        assertEquals("ASSIGNED", statusAfterAssign, "Step 19: MySQL status not updated to ASSIGNED");

        // 20. Verify status history in MySQL
        Integer historyCount1 = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM issue_status_history WHERE issue_id = ?",
                Integer.class,
                issueId
        );
        assertEquals(1, historyCount1, "Step 20: Exactly 1 history record should exist");

        Map<String, Object> h1 = jdbcTemplate.queryForMap(
                "SELECT old_status, new_status, remarks FROM issue_status_history WHERE issue_id = ?",
                issueId
        );
        assertEquals("NEW", h1.get("old_status"));
        assertEquals("ASSIGNED", h1.get("new_status"));
        assertEquals("Assigned to Ward 4 Public Works Engineering Division", h1.get("remarks"));

        // 21. Change status to IN_PROGRESS
        UpdateStatusRequest progressReq = new UpdateStatusRequest("IN_PROGRESS", "Road repair crew dispatched with asphalt mixer");
        ResponseEntity<StatusUpdateResponse> progressRes = restTemplate.exchange(
                baseUrl + "/api/admin/issues/" + issueId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(progressReq, createAuthHeaders(adminToken)),
                StatusUpdateResponse.class
        );
        assertEquals(HttpStatus.OK, progressRes.getStatusCode(), "Step 21: Status update to IN_PROGRESS failed");

        // 22. Change status to RESOLVED
        UpdateStatusRequest resolveReq = new UpdateStatusRequest("RESOLVED", "Pothole filled, asphalt compacted, road fully restored");
        ResponseEntity<StatusUpdateResponse> resolveRes = restTemplate.exchange(
                baseUrl + "/api/admin/issues/" + issueId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(resolveReq, createAuthHeaders(adminToken)),
                StatusUpdateResponse.class
        );
        assertEquals(HttpStatus.OK, resolveRes.getStatusCode(), "Step 22: Status update to RESOLVED failed");

        // 23. Verify all status history records in MySQL
        Integer totalHistory = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM issue_status_history WHERE issue_id = ?",
                Integer.class,
                issueId
        );
        assertEquals(3, totalHistory, "Step 23: Exactly 3 status history records must be persisted in MySQL");

        List<Map<String, Object>> allHistoryRows = jdbcTemplate.queryForList(
                "SELECT old_status, new_status, remarks FROM issue_status_history WHERE issue_id = ? ORDER BY id ASC",
                issueId
        );
        // Transition 1: NEW -> ASSIGNED
        assertEquals("NEW", allHistoryRows.get(0).get("old_status"));
        assertEquals("ASSIGNED", allHistoryRows.get(0).get("new_status"));
        // Transition 2: ASSIGNED -> IN_PROGRESS
        assertEquals("ASSIGNED", allHistoryRows.get(1).get("old_status"));
        assertEquals("IN_PROGRESS", allHistoryRows.get(1).get("new_status"));
        // Transition 3: IN_PROGRESS -> RESOLVED
        assertEquals("IN_PROGRESS", allHistoryRows.get(2).get("old_status"));
        assertEquals("RESOLVED", allHistoryRows.get(2).get("new_status"));

        // ==========================================
        // CITIZEN VERIFICATION PHASE
        // ==========================================

        // 24. Login again as citizen
        ResponseEntity<Map> citizenLoginAgain = restTemplate.postForEntity(
                baseUrl + "/api/auth/login",
                new HttpEntity<>(loginReq),
                Map.class
        );
        assertEquals(HttpStatus.OK, citizenLoginAgain.getStatusCode(), "Step 24: Citizen re-login failed");
        String citizenTokenAgain = (String) citizenLoginAgain.getBody().get("token");

        // 25. Open the issue
        ResponseEntity<IssueResponse> citizenViewResolved = restTemplate.exchange(
                baseUrl + "/api/issues/" + issueId,
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(citizenTokenAgain)),
                IssueResponse.class
        );
        assertEquals(HttpStatus.OK, citizenViewResolved.getStatusCode(), "Step 25: Citizen failed to view updated issue");

        // 26. Verify updated status
        assertEquals(IssueStatus.RESOLVED, citizenViewResolved.getBody().getStatus(), "Step 26: Citizen sees updated status RESOLVED");

        // 27. Verify status history accessible to citizen
        ResponseEntity<List<IssueStatusHistoryResponse>> citizenHistoryRes = restTemplate.exchange(
                baseUrl + "/api/issues/" + issueId + "/history",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(citizenTokenAgain)),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, citizenHistoryRes.getStatusCode(), "Step 27: Citizen status history retrieval failed");
        assertEquals(3, citizenHistoryRes.getBody().size(), "Step 27: Citizen must see all 3 lifecycle transitions");
        assertEquals(IssueStatus.RESOLVED, citizenHistoryRes.getBody().get(2).getNewStatus());
    }

    @Test
    @DisplayName("Negative Tests: Security, Validation, and Authorization boundaries")
    void testAllNegativeCases() {
        String citizenEmail = "neg.citizen@example.com";
        String citizenPassword = "ValidPassword@123";

        // Register a valid citizen for negative testing
        RegisterRequest validCitizen = new RegisterRequest();
        validCitizen.setName("Negative Test Citizen");
        validCitizen.setEmail(citizenEmail);
        validCitizen.setPhone("9876543211");
        validCitizen.setPassword(citizenPassword);

        restTemplate.postForEntity(baseUrl + "/api/auth/register", new HttpEntity<>(validCitizen), Map.class);

        // Get citizen token
        LoginRequest citLogin = new LoginRequest();
        citLogin.setEmail(citizenEmail);
        citLogin.setPassword(citizenPassword);
        String citizenToken = (String) restTemplate.postForEntity(baseUrl + "/api/auth/login", new HttpEntity<>(citLogin), Map.class).getBody().get("token");

        // 1. Invalid registration (empty email / short password)
        RegisterRequest invalidReg = new RegisterRequest();
        invalidReg.setName("Bad User");
        invalidReg.setEmail("bademail");
        invalidReg.setPassword("123");
        ResponseEntity<Map> badRegRes = restTemplate.postForEntity(baseUrl + "/api/auth/register", new HttpEntity<>(invalidReg), Map.class);
        assertEquals(HttpStatus.BAD_REQUEST, badRegRes.getStatusCode(), "Negative: Invalid registration must return 400");

        // 2. Duplicate registration
        ResponseEntity<Map> dupRegRes = restTemplate.postForEntity(baseUrl + "/api/auth/register", new HttpEntity<>(validCitizen), Map.class);
        assertTrue(dupRegRes.getStatusCode() == HttpStatus.CONFLICT || dupRegRes.getStatusCode() == HttpStatus.BAD_REQUEST,
                "Negative: Duplicate registration must return 409 Conflict or 400 Bad Request");

        // 3. Invalid login (wrong password)
        LoginRequest wrongPassReq = new LoginRequest();
        wrongPassReq.setEmail(citizenEmail);
        wrongPassReq.setPassword("IncorrectPassword999");
        ResponseEntity<Map> wrongPassRes = restTemplate.postForEntity(baseUrl + "/api/auth/login", new HttpEntity<>(wrongPassReq), Map.class);
        assertEquals(HttpStatus.UNAUTHORIZED, wrongPassRes.getStatusCode(), "Negative: Wrong password must return 401");

        // 4. Missing authentication
        ResponseEntity<Map> noAuthRes = restTemplate.exchange(
                baseUrl + "/api/admin/dashboard",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(null)),
                Map.class
        );
        assertEquals(HttpStatus.UNAUTHORIZED, noAuthRes.getStatusCode(), "Negative: Missing auth token must return 401");

        // 5. Invalid JWT
        ResponseEntity<Map> badJwtRes = restTemplate.exchange(
                baseUrl + "/api/issues/my",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders("invalid.fake.jwt.token")),
                Map.class
        );
        assertEquals(HttpStatus.UNAUTHORIZED, badJwtRes.getStatusCode(), "Negative: Forged/invalid JWT must return 401");

        // 6. Citizen accessing admin endpoint (403 Forbidden)
        ResponseEntity<Map> citAdminForbidden = restTemplate.exchange(
                baseUrl + "/api/admin/dashboard",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(citizenToken)),
                Map.class
        );
        assertEquals(HttpStatus.FORBIDDEN, citAdminForbidden.getStatusCode(), "Negative: Citizen on admin endpoint must return 403");

        // 7. Missing issue (404 Not Found)
        ResponseEntity<Map> notFoundRes = restTemplate.exchange(
                baseUrl + "/api/issues/999999",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(citizenToken)),
                Map.class
        );
        assertEquals(HttpStatus.NOT_FOUND, notFoundRes.getStatusCode(), "Negative: Non-existent issue must return 404");

        // 8. Invalid category (400 Bad Request)
        CreateIssueRequest badCatReq = new CreateIssueRequest();
        badCatReq.setTitle("Valid Title");
        badCatReq.setCategory("INVALID_CATEGORY_XYZ");
        badCatReq.setLocation("Valid Location");
        badCatReq.setPriority("LOW");
        badCatReq.setDescription("Valid Description");
        ResponseEntity<Map> badCatRes = restTemplate.postForEntity(
                baseUrl + "/api/issues",
                new HttpEntity<>(badCatReq, createAuthHeaders(citizenToken)),
                Map.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, badCatRes.getStatusCode(), "Negative: Invalid category must return 400");

        // 9. Invalid priority (400 Bad Request)
        CreateIssueRequest badPriReq = new CreateIssueRequest();
        badPriReq.setTitle("Valid Title");
        badPriReq.setCategory("ROAD");
        badPriReq.setLocation("Valid Location");
        badPriReq.setPriority("SUPER_URGENT_NOT_AN_ENUM");
        badPriReq.setDescription("Valid Description");
        ResponseEntity<Map> badPriRes = restTemplate.postForEntity(
                baseUrl + "/api/issues",
                new HttpEntity<>(badPriReq, createAuthHeaders(citizenToken)),
                Map.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, badPriRes.getStatusCode(), "Negative: Invalid priority must return 400");

        // 10. Missing required fields (blank title)
        CreateIssueRequest blankTitleReq = new CreateIssueRequest();
        blankTitleReq.setTitle("   ");
        blankTitleReq.setCategory("ROAD");
        blankTitleReq.setLocation("Valid Location");
        blankTitleReq.setPriority("LOW");
        blankTitleReq.setDescription("Valid Description");
        ResponseEntity<Map> blankTitleRes = restTemplate.postForEntity(
                baseUrl + "/api/issues",
                new HttpEntity<>(blankTitleReq, createAuthHeaders(citizenToken)),
                Map.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, blankTitleRes.getStatusCode(), "Negative: Blank title must return 400");

        // Create an issue by Citizen A
        CreateIssueRequest citAIssueReq = new CreateIssueRequest();
        citAIssueReq.setTitle("Citizen A Pothole");
        citAIssueReq.setCategory("ROAD");
        citAIssueReq.setLocation("Location A");
        citAIssueReq.setPriority("MEDIUM");
        citAIssueReq.setDescription("Description A");
        IssueResponse citAIssue = restTemplate.postForEntity(
                baseUrl + "/api/issues",
                new HttpEntity<>(citAIssueReq, createAuthHeaders(citizenToken)),
                IssueResponse.class
        ).getBody();
        assertNotNull(citAIssue);

        // 11. Invalid status update (Admin trying to set unsupported status)
        LoginRequest adminLogin = new LoginRequest();
        adminLogin.setEmail(adminEmail);
        adminLogin.setPassword(adminPassword);
        String adminToken = (String) restTemplate.postForEntity(baseUrl + "/api/auth/login", new HttpEntity<>(adminLogin), Map.class).getBody().get("token");

        UpdateStatusRequest badStatusReq = new UpdateStatusRequest("FAKE_STATUS", "Invalid test");
        ResponseEntity<Map> badStatusRes = restTemplate.exchange(
                baseUrl + "/api/admin/issues/" + citAIssue.getId() + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(badStatusReq, createAuthHeaders(adminToken)),
                Map.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, badStatusRes.getStatusCode(), "Negative: Invalid status name must return 400");

        // 12. Unauthorized issue access: Citizen B trying to view Citizen A's issue (403 Forbidden)
        RegisterRequest citizenBReq = new RegisterRequest();
        citizenBReq.setName("Citizen B");
        citizenBReq.setEmail("citizen.b@example.com");
        citizenBReq.setPhone("9876543299");
        citizenBReq.setPassword("PasswordB@123");
        restTemplate.postForEntity(baseUrl + "/api/auth/register", new HttpEntity<>(citizenBReq), Map.class);

        LoginRequest bLogin = new LoginRequest();
        bLogin.setEmail("citizen.b@example.com");
        bLogin.setPassword("PasswordB@123");
        String citizenBToken = (String) restTemplate.postForEntity(baseUrl + "/api/auth/login", new HttpEntity<>(bLogin), Map.class).getBody().get("token");

        ResponseEntity<Map> citBAccessCitAIssue = restTemplate.exchange(
                baseUrl + "/api/issues/" + citAIssue.getId(),
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(citizenBToken)),
                Map.class
        );
        assertEquals(HttpStatus.FORBIDDEN, citBAccessCitAIssue.getStatusCode(), "Negative: Citizen B accessing Citizen A's issue must return 403 Forbidden");
    }
}
