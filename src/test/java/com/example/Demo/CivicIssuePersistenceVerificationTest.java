package com.example.Demo;

import com.example.Demo.dto.CreateIssueRequest;
import com.example.Demo.dto.IssueResponse;
import com.example.Demo.security.JwtUtil;
import org.junit.jupiter.api.Assertions;
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
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CivicIssuePersistenceVerificationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired(required = false)
    private IssueStatusHistoryRepository issueStatusHistoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private static final String CITIZEN_EMAIL = "citizen.demo@civic.gov.in";

    @org.junit.jupiter.api.BeforeEach
    @org.junit.jupiter.api.AfterEach
    void cleanUp() {
        if (issueStatusHistoryRepository != null) {
            issueStatusHistoryRepository.deleteAll();
        }
        issueRepository.deleteAll();
    }

    @Test
    @DisplayName("Verify Citizen submits issue -> Spring Boot -> Service -> Repository -> Hibernate -> MySQL")
    void testCitizenSubmitsIssueAndPersistsInMySQL() {
        // Ensure Ramesh Kumar exists
        Users citizen = userRepository.findByEmail(CITIZEN_EMAIL).orElseGet(() -> {
            Users u = new Users();
            u.setName("Ramesh Kumar");
            u.setEmail(CITIZEN_EMAIL);
            u.setPassword(passwordEncoder.encode("Citizen@1234"));
            u.setRole(Role.CITIZEN);
            return userRepository.save(u);
        });

        String token = jwtUtil.generateToken(citizen);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);

        CreateIssueRequest request = new CreateIssueRequest(
                "Broken Traffic Signal at MG Road Junction",
                "Traffic lights have been stuck on flashing red since yesterday evening, causing severe traffic jams during peak hours.",
                "TRAFFIC",
                "MG Road and 4th Cross Junction, Ward 15",
                "HIGH",
                "https://civic-portal.local/uploads/traffic_light_mg_road.jpg"
        );

        HttpEntity<CreateIssueRequest> entity = new HttpEntity<>(request, headers);
        String url = "http://localhost:" + port + "/api/issues";

        ResponseEntity<IssueResponse> response = restTemplate.postForEntity(url, entity, IssueResponse.class);

        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Integer issueId = response.getBody().getId();
        Assertions.assertNotNull(issueId);

        // Verify through repository
        Issue persisted = issueRepository.findById(issueId).orElse(null);
        Assertions.assertNotNull(persisted);
        Assertions.assertEquals("Broken Traffic Signal at MG Road Junction", persisted.getTitle());
        Assertions.assertEquals(IssueCategory.TRAFFIC, persisted.getCategory());
        Assertions.assertEquals(IssuePriority.HIGH, persisted.getPriority());
        Assertions.assertEquals(IssueStatus.NEW, persisted.getStatus());
        Assertions.assertEquals(citizen.getId(), persisted.getReportedBy().getId());
        Assertions.assertNotNull(persisted.getCreatedAt());

        System.out.println(">>> SUCCESSFULLY PERSISTED CIVIC ISSUE ID: " + issueId + " IN MYSQL <<<");
    }
}
