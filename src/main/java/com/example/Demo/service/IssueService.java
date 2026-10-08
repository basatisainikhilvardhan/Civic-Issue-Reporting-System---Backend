package com.example.Demo.service;

import com.example.Demo.Issue;
import com.example.Demo.IssueCategory;
import com.example.Demo.IssuePriority;
import com.example.Demo.IssueRepository;
import com.example.Demo.IssueStatus;
import com.example.Demo.IssueStatusHistory;
import com.example.Demo.IssueStatusHistoryRepository;
import com.example.Demo.Role;
import com.example.Demo.UserRepository;
import com.example.Demo.Users;
import com.example.Demo.dto.CreateIssueRequest;
import com.example.Demo.dto.DashboardStatsResponse;
import com.example.Demo.dto.IssueResponse;
import com.example.Demo.dto.IssueStatusHistoryResponse;
import com.example.Demo.dto.StatusUpdateResponse;
import com.example.Demo.dto.UpdateIssueRequest;
import com.example.Demo.dto.UpdateStatusRequest;
import com.example.Demo.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IssueService {

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private IssueStatusHistoryRepository issueStatusHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    private Users getUserByEmailOrUsername(String emailOrUsername) {
        return userRepository.findByEmail(emailOrUsername)
                .orElseGet(() -> {
                    Users u = userRepository.findByUsername(emailOrUsername);
                    if (u == null) {
                        throw new UsernameNotFoundException("User not found: " + emailOrUsername);
                    }
                    return u;
                });
    }

    private IssueCategory parseCategory(String categoryStr) {
        if (categoryStr == null || categoryStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Category is required");
        }
        try {
            return IssueCategory.valueOf(categoryStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid category: '" + categoryStr + "'. Allowed categories: ROAD, GARBAGE, STREET_LIGHT, WATER, DRAINAGE, ELECTRICITY, PUBLIC_PROPERTY, TRAFFIC, OTHER");
        }
    }

    private IssuePriority parsePriority(String priorityStr) {
        if (priorityStr == null || priorityStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Priority is required");
        }
        try {
            return IssuePriority.valueOf(priorityStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid priority: '" + priorityStr + "'. Allowed priorities: LOW, MEDIUM, HIGH, CRITICAL");
        }
    }

    @Transactional
    public IssueResponse createIssue(CreateIssueRequest request, String userEmail) {
        if (request == null) {
            throw new IllegalArgumentException("Issue request cannot be empty");
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required");
        }
        if (request.getLocation() == null || request.getLocation().trim().isEmpty()) {
            throw new IllegalArgumentException("Location is required");
        }

        IssueCategory category = parseCategory(request.getCategory());
        IssuePriority priority = parsePriority(request.getPriority());

        Users currentUser = getUserByEmailOrUsername(userEmail);

        Issue issue = new Issue();
        issue.setTitle(request.getTitle().trim());
        issue.setDescription(request.getDescription().trim());
        issue.setCategory(category);
        issue.setLocation(request.getLocation().trim());
        issue.setPriority(priority);
        // Requirement: New issues must start as NEW. Citizen cannot arbitrarily choose initial status.
        issue.setStatus(IssueStatus.NEW);
        issue.setImageUrl(request.getImageUrl() != null ? request.getImageUrl().trim() : null);
        issue.setReportedBy(currentUser);
        issue.setCreatedAt(LocalDateTime.now());
        issue.setUpdatedAt(LocalDateTime.now());

        Issue saved = issueRepository.save(issue);
        return new IssueResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<IssueResponse> getMyIssues(String userEmail) {
        Users currentUser = getUserByEmailOrUsername(userEmail);
        return issueRepository.findByReportedByOrderByCreatedAtDesc(currentUser)
                .stream()
                .map(IssueResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IssueResponse> getAllIssuesForAdmin() {
        return issueRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(IssueResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public IssueResponse getIssueById(Integer id, String userEmail) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with id: " + id));

        Users currentUser = getUserByEmailOrUsername(userEmail);
        boolean isOwner = issue.getReportedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to view this issue");
        }

        return new IssueResponse(issue);
    }

    @Transactional
    public IssueResponse updateIssue(Integer id, UpdateIssueRequest request, String userEmail) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with id: " + id));

        Users currentUser = getUserByEmailOrUsername(userEmail);
        boolean isOwner = issue.getReportedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to update this issue");
        }

        if (issue.getStatus() != IssueStatus.NEW && !isAdmin) {
            throw new IllegalArgumentException("Cannot modify an issue that is already " + issue.getStatus());
        }

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            issue.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
            issue.setDescription(request.getDescription().trim());
        }
        if (request.getLocation() != null && !request.getLocation().trim().isEmpty()) {
            issue.setLocation(request.getLocation().trim());
        }
        if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
            issue.setCategory(parseCategory(request.getCategory()));
        }
        if (request.getPriority() != null && !request.getPriority().trim().isEmpty()) {
            issue.setPriority(parsePriority(request.getPriority()));
        }
        if (request.getImageUrl() != null) {
            issue.setImageUrl(request.getImageUrl().trim());
        }

        issue.setUpdatedAt(LocalDateTime.now());
        Issue updated = issueRepository.save(issue);
        return new IssueResponse(updated);
    }

    @Transactional
    public void deleteIssue(Integer id, String userEmail) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with id: " + id));

        Users currentUser = getUserByEmailOrUsername(userEmail);
        boolean isOwner = issue.getReportedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to delete this issue");
        }

        if (issue.getStatus() != IssueStatus.NEW && !isAdmin) {
            throw new IllegalArgumentException("Cannot delete an issue that is already " + issue.getStatus());
        }

        issueRepository.delete(issue);
    }

    /**
     * Admin status update workflow:
     * 1. Authenticate admin.
     * 2. Find the issue.
     * 3. Validate the requested status.
     * 4. Record old status.
     * 5. Update new status.
     * 6. Save the issue.
     * 7. Create status history.
     * 8. Save status history.
     * 9. Return updated information.
     */
    @Transactional
    public StatusUpdateResponse updateIssueStatus(Integer issueId, UpdateStatusRequest request, String adminEmail) {
        // 1. Authenticate / verify admin
        Users adminUser = getUserByEmailOrUsername(adminEmail);
        if (adminUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Access denied: Admin authorization required");
        }

        // 2. Find the issue
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with id: " + issueId));

        // 3. Validate the requested status
        if (request == null || request.getStatus() == null || request.getStatus().trim().isEmpty()) {
            throw new IllegalArgumentException("Status is required");
        }

        IssueStatus newStatus;
        try {
            newStatus = IssueStatus.valueOf(request.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: '" + request.getStatus() + "'. Allowed statuses: NEW, ASSIGNED, IN_PROGRESS, RESOLVED, REJECTED");
        }

        // 4. Record old status
        IssueStatus oldStatus = issue.getStatus();
        if (oldStatus == newStatus) {
            throw new IllegalArgumentException("Issue is already in status " + newStatus);
        }

        // 5. Update new status
        issue.setStatus(newStatus);
        issue.setUpdatedAt(LocalDateTime.now());

        // 6. Save the issue
        Issue savedIssue = issueRepository.save(issue);

        // 7. Create status history
        IssueStatusHistory history = new IssueStatusHistory(
                savedIssue,
                oldStatus,
                newStatus,
                request.getRemarks() != null ? request.getRemarks().trim() : null,
                adminUser
        );

        // 8. Save status history
        IssueStatusHistory savedHistory = issueStatusHistoryRepository.save(history);

        // 9. Return updated information
        List<IssueStatusHistory> allHistories = issueStatusHistoryRepository.findByIssueIdOrderByIdAsc(savedIssue.getId());
        return new StatusUpdateResponse(savedIssue, savedHistory, allHistories);
    }

    @Transactional(readOnly = true)
    public List<IssueStatusHistoryResponse> getIssueStatusHistory(Integer issueId, String userEmail) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with id: " + issueId));

        Users currentUser = getUserByEmailOrUsername(userEmail);
        boolean isOwner = issue.getReportedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to view this issue's history");
        }

        return issueStatusHistoryRepository.findByIssueIdOrderByIdAsc(issueId)
                .stream()
                .map(IssueStatusHistoryResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStatistics() {
        long totalIssues = issueRepository.count();
        long newCount = issueRepository.countByStatus(IssueStatus.NEW);
        long assignedCount = issueRepository.countByStatus(IssueStatus.ASSIGNED);
        long inProgressCount = issueRepository.countByStatus(IssueStatus.IN_PROGRESS);
        long resolvedCount = issueRepository.countByStatus(IssueStatus.RESOLVED);
        long rejectedCount = issueRepository.countByStatus(IssueStatus.REJECTED);

        return new DashboardStatsResponse(
                totalIssues,
                newCount,
                assignedCount,
                inProgressCount,
                resolvedCount,
                rejectedCount
        );
    }

    public String saveUploadedPhoto(org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No photo file selected for upload.");
        }

        // Enforce 5 MB limit
        long maxSizeBytes = 5 * 1024 * 1024;
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("Photo file size exceeds maximum limit of 5 MB.");
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equalsIgnoreCase("image/jpeg") 
                || contentType.equalsIgnoreCase("image/jpg") 
                || contentType.equalsIgnoreCase("image/png"))) {
            throw new IllegalArgumentException("Unsupported image format. Allowed formats: JPG, JPEG, PNG.");
        }

        // Validate original filename extension
        String originalFilename = file.getOriginalFilename();
        String extension = "jpg";
        if (originalFilename != null && originalFilename.lastIndexOf('.') > 0) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
            if (ext.equals("png") || ext.equals("jpeg") || ext.equals("jpg")) {
                extension = ext;
            } else {
                throw new IllegalArgumentException("Unsupported image extension. Allowed extensions: .jpg, .jpeg, .png");
            }
        }

        java.io.File uploadDir = new java.io.File("uploads");
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        String safeFileName = java.util.UUID.randomUUID().toString() + "." + extension;
        java.nio.file.Path targetPath = uploadDir.toPath().resolve(safeFileName);

        java.nio.file.Files.copy(file.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/" + safeFileName;
    }
}
