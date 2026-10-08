package com.example.Demo;

import com.example.Demo.dto.CreateIssueRequest;
import com.example.Demo.dto.IssueResponse;
import com.example.Demo.dto.StatusUpdateResponse;
import com.example.Demo.dto.UpdateStatusRequest;
import com.example.Demo.security.JwtUtil;
import java.util.List;
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
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LiveStatusTransitionVerificationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private IssueStatusHistoryRepository issueStatusHistoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private static final String CITIZEN_EMAIL = "citizen.demo@civic.gov.in";
    private static final String ADMIN_EMAIL = "admin.civic@civic.gov.in";

    @BeforeEach
    @AfterEach
    void cleanUp() {
        issueStatusHistoryRepository.deleteAll();
        issueRepository.deleteAll();
    }

    @Test
    @DisplayName("Verify Full Lifecycle: NEW -> ASSIGNED -> IN_PROGRESS -> RESOLVED persisted in MySQL")
    void testFullStatusLifecycleWithHistoryPersistence() {
        // 1. Ensure Ramesh Kumar exists
        Users citizen = userRepository.findByEmail(CITIZEN_EMAIL).orElseGet(() -> {
            Users u = new Users();
            u.setName("Ramesh Kumar");
            u.setEmail(CITIZEN_EMAIL);
            u.setPassword(passwordEncoder.encode("Citizen@1234"));
            u.setRole(Role.CITIZEN);
            return userRepository.save(u);
        });

        // 2. Ensure Admin exists
        Users admin = userRepository.findByEmail(ADMIN_EMAIL).orElseGet(() -> {
            Users a = new Users();
            a.setName("Civic Municipal Administrator");
            a.setEmail(ADMIN_EMAIL);
            a.setPassword(passwordEncoder.encode("Admin@1234"));
            a.setRole(Role.ADMIN);
            return userRepository.save(a);
        });

        String citizenToken = jwtUtil.generateToken(citizen);
        String adminToken = jwtUtil.generateToken(admin);

        RestTemplate restTemplate = new RestTemplate();

        // 3. Citizen reports an issue
        HttpHeaders citizenHeaders = new HttpHeaders();
        citizenHeaders.setContentType(MediaType.APPLICATION_JSON);
        citizenHeaders.set("Authorization", "Bearer " + citizenToken);

        CreateIssueRequest createReq = new CreateIssueRequest(
                "Flooded Underpass near Railway Station",
                "Severe waterlogging in railway underpass after heavy rainfall. Vehicles unable to pass.",
                "DRAINAGE",
                "City Railway Station Underpass, Ward 4",
                "CRITICAL",
                "https://civic-portal.local/uploads/underpass_flooding.jpg"
        );

        ResponseEntity<IssueResponse> createRes = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/issues",
                new HttpEntity<>(createReq, citizenHeaders),
                IssueResponse.class
        );

        Assertions.assertEquals(HttpStatus.CREATED, createRes.getStatusCode());
        Integer issueId = createRes.getBody().getId();
        Assertions.assertEquals(IssueStatus.NEW, createRes.getBody().getStatus());

        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setContentType(MediaType.APPLICATION_JSON);
        adminHeaders.set("Authorization", "Bearer " + adminToken);

        String adminUrl = "http://localhost:" + port + "/api/admin/issues/" + issueId + "/status";

        // Transition 1: NEW -> ASSIGNED
        UpdateStatusRequest req1 = new UpdateStatusRequest("ASSIGNED", "Assigned to Stormwater Drainage Rapid Action Unit");
        ResponseEntity<StatusUpdateResponse> res1 = restTemplate.exchange(adminUrl, HttpMethod.PUT, new HttpEntity<>(req1, adminHeaders), StatusUpdateResponse.class);
        Assertions.assertEquals(HttpStatus.OK, res1.getStatusCode());
        Assertions.assertEquals(IssueStatus.ASSIGNED, res1.getBody().getStatus());

        // Transition 2: ASSIGNED -> IN_PROGRESS
        UpdateStatusRequest req2 = new UpdateStatusRequest("IN_PROGRESS", "High-capacity suction dewatering pumps deployed at underpass");
        ResponseEntity<StatusUpdateResponse> res2 = restTemplate.exchange(adminUrl, HttpMethod.PUT, new HttpEntity<>(req2, adminHeaders), StatusUpdateResponse.class);
        Assertions.assertEquals(HttpStatus.OK, res2.getStatusCode());
        Assertions.assertEquals(IssueStatus.IN_PROGRESS, res2.getBody().getStatus());

        // Transition 3: IN_PROGRESS -> RESOLVED
        UpdateStatusRequest req3 = new UpdateStatusRequest("RESOLVED", "All flood water pumped out, drain blockages cleared, and traffic resumed safely.");
        ResponseEntity<StatusUpdateResponse> res3 = restTemplate.exchange(adminUrl, HttpMethod.PUT, new HttpEntity<>(req3, adminHeaders), StatusUpdateResponse.class);
        Assertions.assertEquals(HttpStatus.OK, res3.getStatusCode());
        Assertions.assertEquals(IssueStatus.RESOLVED, res3.getBody().getStatus());

        // 4. Verify in MySQL via Repositories
        Issue issueInDb = issueRepository.findById(issueId).orElse(null);
        Assertions.assertNotNull(issueInDb);
        Assertions.assertEquals(IssueStatus.RESOLVED, issueInDb.getStatus());

        List<IssueStatusHistory> histories = issueStatusHistoryRepository.findByIssueIdOrderByIdAsc(issueId);
        Assertions.assertEquals(3, histories.size());

        Assertions.assertEquals(IssueStatus.NEW, histories.get(0).getOldStatus());
        Assertions.assertEquals(IssueStatus.ASSIGNED, histories.get(0).getNewStatus());

        Assertions.assertEquals(IssueStatus.ASSIGNED, histories.get(1).getOldStatus());
        Assertions.assertEquals(IssueStatus.IN_PROGRESS, histories.get(1).getNewStatus());

        Assertions.assertEquals(IssueStatus.IN_PROGRESS, histories.get(2).getOldStatus());
        Assertions.assertEquals(IssueStatus.RESOLVED, histories.get(2).getNewStatus());

        System.out.println(">>> FULL LIFECYCLE AND 3 HISTORY RECORDS PERSISTED FOR ISSUE ID: " + issueId + " IN MYSQL <<<");
    }
}
