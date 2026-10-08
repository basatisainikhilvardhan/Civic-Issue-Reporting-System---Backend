package com.example.Demo;

import com.example.Demo.dto.DashboardStatsResponse;
import com.example.Demo.dto.UpdateStatusRequest;
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
public class AdminDashboardTest {

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
    private Users citizenUser;
    private String adminToken;
    private String citizenToken;

    private static final String ADMIN_EMAIL = "admin_dashboard_test@civic.gov.in";
    private static final String CITIZEN_EMAIL = "citizen_dashboard_test@civic.gov.in";

    private String getDashboardUrl() {
        return "http://localhost:" + port + "/api/admin/dashboard";
    }

    private String getStatusUpdateUrl(Integer id) {
        return "http://localhost:" + port + "/api/admin/issues/" + id + "/status";
    }

    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.set("Authorization", "Bearer " + token);
        }
        return headers;
    }

    private void cleanUpDatabase() {
        issueStatusHistoryRepository.deleteAll();
        issueRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();

        cleanUpDatabase();

        userRepository.findByEmail(ADMIN_EMAIL).ifPresent(userRepository::delete);
        userRepository.findByEmail(CITIZEN_EMAIL).ifPresent(userRepository::delete);

        // 1. Create Admin
        adminUser = new Users();
        adminUser.setName("Dashboard Admin");
        adminUser.setEmail(ADMIN_EMAIL);
        adminUser.setPassword(passwordEncoder.encode("AdminPass123!"));
        adminUser.setRole(Role.ADMIN);
        adminUser = userRepository.save(adminUser);
        adminToken = jwtUtil.generateToken(adminUser);

        // 2. Create Citizen
        citizenUser = new Users();
        citizenUser.setName("Dashboard Citizen");
        citizenUser.setEmail(CITIZEN_EMAIL);
        citizenUser.setPassword(passwordEncoder.encode("CitPass123!"));
        citizenUser.setRole(Role.CITIZEN);
        citizenUser = userRepository.save(citizenUser);
        citizenToken = jwtUtil.generateToken(citizenUser);
    }

    @AfterEach
    void tearDown() {
        cleanUpDatabase();

        if (adminUser != null && adminUser.getXyz() != null) {
            userRepository.findById(adminUser.getXyz()).ifPresent(userRepository::delete);
        }
        if (citizenUser != null && citizenUser.getXyz() != null) {
            userRepository.findById(citizenUser.getXyz()).ifPresent(userRepository::delete);
        }
    }

    private Issue createIssueWithStatus(String title, IssueCategory category, IssuePriority priority, IssueStatus status) {
        Issue issue = new Issue();
        issue.setTitle(title);
        issue.setDescription("Description for " + title);
        issue.setCategory(category);
        issue.setLocation("Test Location");
        issue.setPriority(priority);
        issue.setStatus(status);
        issue.setReportedBy(citizenUser);
        return issueRepository.save(issue);
    }

    @Test
    @DisplayName("1. Dashboard Returns Real MySQL Counts for Known Records")
    void test1_dashboardReturnsRealCounts() {
        // Create known distribution of test records:
        // 2 NEW, 1 ASSIGNED, 2 IN_PROGRESS, 1 RESOLVED, 1 REJECTED = Total 7
        createIssueWithStatus("Pothole 1", IssueCategory.ROAD, IssuePriority.HIGH, IssueStatus.NEW);
        createIssueWithStatus("Pothole 2", IssueCategory.ROAD, IssuePriority.MEDIUM, IssueStatus.NEW);
        createIssueWithStatus("Streetlight Out", IssueCategory.STREET_LIGHT, IssuePriority.LOW, IssueStatus.ASSIGNED);
        createIssueWithStatus("Water Pipe Leak 1", IssueCategory.WATER, IssuePriority.HIGH, IssueStatus.IN_PROGRESS);
        createIssueWithStatus("Water Pipe Leak 2", IssueCategory.WATER, IssuePriority.CRITICAL, IssueStatus.IN_PROGRESS);
        createIssueWithStatus("Garbage Cleaned", IssueCategory.GARBAGE, IssuePriority.MEDIUM, IssueStatus.RESOLVED);
        createIssueWithStatus("Duplicate Drainage Report", IssueCategory.DRAINAGE, IssuePriority.LOW, IssueStatus.REJECTED);

        // Fetch dashboard statistics
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(adminToken));
        ResponseEntity<DashboardStatsResponse> response = restTemplate.exchange(
                getDashboardUrl(),
                HttpMethod.GET,
                entity,
                DashboardStatsResponse.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        DashboardStatsResponse stats = response.getBody();
        Assertions.assertNotNull(stats);

        // Verify exact counts match database
        Assertions.assertEquals(7, stats.getTotalIssues(), "Total issues must be 7");
        Assertions.assertEquals(2, stats.getNewIssues(), "NEW count must be 2");
        Assertions.assertEquals(1, stats.getAssignedIssues(), "ASSIGNED count must be 1");
        Assertions.assertEquals(2, stats.getInProgressIssues(), "IN_PROGRESS count must be 2");
        Assertions.assertEquals(1, stats.getResolvedIssues(), "RESOLVED count must be 1");
        Assertions.assertEquals(1, stats.getRejectedIssues(), "REJECTED count must be 1");

        // Verify direct repository counts match API response
        Assertions.assertEquals(issueRepository.count(), stats.getTotalIssues());
        Assertions.assertEquals(issueRepository.countByStatus(IssueStatus.NEW), stats.getNewIssues());
        Assertions.assertEquals(issueRepository.countByStatus(IssueStatus.ASSIGNED), stats.getAssignedIssues());
        Assertions.assertEquals(issueRepository.countByStatus(IssueStatus.IN_PROGRESS), stats.getInProgressIssues());
        Assertions.assertEquals(issueRepository.countByStatus(IssueStatus.RESOLVED), stats.getResolvedIssues());
        Assertions.assertEquals(issueRepository.countByStatus(IssueStatus.REJECTED), stats.getRejectedIssues());
    }

    @Test
    @DisplayName("2. Dashboard Count Dynamically Updates When Issue Status Changes")
    void test2_dashboardCountsChangeDynamicallyOnStatusTransition() {
        // Create known records: 1 NEW, 0 ASSIGNED, 0 IN_PROGRESS, 0 RESOLVED, 0 REJECTED
        Issue issue = createIssueWithStatus("Open Drain Hazard", IssueCategory.DRAINAGE, IssuePriority.CRITICAL, IssueStatus.NEW);

        // Step 1: Initial check
        HttpEntity<Void> getEntity = new HttpEntity<>(createAuthHeaders(adminToken));
        DashboardStatsResponse initialStats = restTemplate.exchange(
                getDashboardUrl(), HttpMethod.GET, getEntity, DashboardStatsResponse.class
        ).getBody();

        Assertions.assertEquals(1, initialStats.getTotalIssues());
        Assertions.assertEquals(1, initialStats.getNewIssues());
        Assertions.assertEquals(0, initialStats.getAssignedIssues());

        // Step 2: Transition issue: NEW -> ASSIGNED
        UpdateStatusRequest updateReq = new UpdateStatusRequest("ASSIGNED", "Assigned to Drainage Emergency Response Team");
        HttpEntity<UpdateStatusRequest> putEntity = new HttpEntity<>(updateReq, createAuthHeaders(adminToken));
        restTemplate.exchange(getStatusUpdateUrl(issue.getId()), HttpMethod.PUT, putEntity, Map.class);

        // Step 3: Verify dashboard immediately reflects NEW: 0, ASSIGNED: 1
        DashboardStatsResponse afterAssignStats = restTemplate.exchange(
                getDashboardUrl(), HttpMethod.GET, getEntity, DashboardStatsResponse.class
        ).getBody();

        Assertions.assertEquals(1, afterAssignStats.getTotalIssues(), "Total count remains 1");
        Assertions.assertEquals(0, afterAssignStats.getNewIssues(), "NEW count should drop to 0");
        Assertions.assertEquals(1, afterAssignStats.getAssignedIssues(), "ASSIGNED count should increase to 1");

        // Step 4: Transition issue: ASSIGNED -> IN_PROGRESS
        updateReq = new UpdateStatusRequest("IN_PROGRESS", "Crew excavating drain");
        putEntity = new HttpEntity<>(updateReq, createAuthHeaders(adminToken));
        restTemplate.exchange(getStatusUpdateUrl(issue.getId()), HttpMethod.PUT, putEntity, Map.class);

        DashboardStatsResponse afterProgressStats = restTemplate.exchange(
                getDashboardUrl(), HttpMethod.GET, getEntity, DashboardStatsResponse.class
        ).getBody();

        Assertions.assertEquals(0, afterProgressStats.getAssignedIssues(), "ASSIGNED count should drop to 0");
        Assertions.assertEquals(1, afterProgressStats.getInProgressIssues(), "IN_PROGRESS count should increase to 1");

        // Step 5: Transition issue: IN_PROGRESS -> RESOLVED
        updateReq = new UpdateStatusRequest("RESOLVED", "Drain covered and reinforced");
        putEntity = new HttpEntity<>(updateReq, createAuthHeaders(adminToken));
        restTemplate.exchange(getStatusUpdateUrl(issue.getId()), HttpMethod.PUT, putEntity, Map.class);

        DashboardStatsResponse afterResolvedStats = restTemplate.exchange(
                getDashboardUrl(), HttpMethod.GET, getEntity, DashboardStatsResponse.class
        ).getBody();

        Assertions.assertEquals(0, afterResolvedStats.getInProgressIssues(), "IN_PROGRESS count should drop to 0");
        Assertions.assertEquals(1, afterResolvedStats.getResolvedIssues(), "RESOLVED count should increase to 1");
        Assertions.assertEquals(1, afterResolvedStats.getTotalIssues(), "Total issues remains 1");
    }

    @Test
    @DisplayName("3. Security: Citizen Cannot Access Dashboard - Returns 403 Forbidden")
    void test3_citizenCannotAccessDashboard() {
        HttpEntity<Void> citizenEntity = new HttpEntity<>(createAuthHeaders(citizenToken));

        try {
            restTemplate.exchange(getDashboardUrl(), HttpMethod.GET, citizenEntity, Map.class);
            Assertions.fail("Expected 403 Forbidden for citizen accessing admin dashboard");
        } catch (HttpClientErrorException.Forbidden ex) {
            Assertions.assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        }
    }

    @Test
    @DisplayName("4. Security: Unauthenticated Request Blocked - Returns 401/403")
    void test4_unauthenticatedRequestBlocked() {
        HttpEntity<Void> noTokenEntity = new HttpEntity<>(createAuthHeaders(null));

        try {
            restTemplate.exchange(getDashboardUrl(), HttpMethod.GET, noTokenEntity, Map.class);
            Assertions.fail("Expected 401/403 for unauthenticated access to admin dashboard");
        } catch (HttpClientErrorException ex) {
            Assertions.assertTrue(ex.getStatusCode() == HttpStatus.UNAUTHORIZED || ex.getStatusCode() == HttpStatus.FORBIDDEN);
        }
    }

    @Test
    @DisplayName("5. Dashboard Zero State: Returns All Zeroes When No Issues Exist")
    void test5_dashboardZeroState() {
        // Clean database has 0 issues
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(adminToken));
        ResponseEntity<DashboardStatsResponse> response = restTemplate.exchange(
                getDashboardUrl(),
                HttpMethod.GET,
                entity,
                DashboardStatsResponse.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        DashboardStatsResponse stats = response.getBody();
        Assertions.assertNotNull(stats);

        Assertions.assertEquals(0, stats.getTotalIssues());
        Assertions.assertEquals(0, stats.getNewIssues());
        Assertions.assertEquals(0, stats.getAssignedIssues());
        Assertions.assertEquals(0, stats.getInProgressIssues());
        Assertions.assertEquals(0, stats.getResolvedIssues());
        Assertions.assertEquals(0, stats.getRejectedIssues());
    }
}
