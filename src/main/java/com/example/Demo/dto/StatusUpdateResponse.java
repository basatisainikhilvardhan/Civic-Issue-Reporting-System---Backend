package com.example.Demo.dto;

import com.example.Demo.Issue;
import com.example.Demo.IssueCategory;
import com.example.Demo.IssuePriority;
import com.example.Demo.IssueStatus;
import com.example.Demo.IssueStatusHistory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StatusUpdateResponse {

    private Integer id;
    private String title;
    private String description;
    private IssueCategory category;
    private String location;
    private IssuePriority priority;
    private IssueStatus oldStatus;
    private IssueStatus newStatus;
    private IssueStatus status;
    private String remarks;
    private Integer changedById;
    private String changedByName;
    private String changedByEmail;
    private LocalDateTime changedAt;
    private LocalDateTime updatedAt;
    private Integer reportedById;
    private String reportedByName;
    private String reportedByEmail;
    private List<IssueStatusHistoryResponse> statusHistory = new ArrayList<>();

    public StatusUpdateResponse() {
    }

    public StatusUpdateResponse(Issue issue, IssueStatusHistory history, List<IssueStatusHistory> histories) {
        if (issue != null) {
            this.id = issue.getId();
            this.title = issue.getTitle();
            this.description = issue.getDescription();
            this.category = issue.getCategory();
            this.location = issue.getLocation();
            this.priority = issue.getPriority();
            this.status = issue.getStatus();
            this.updatedAt = issue.getUpdatedAt();
            if (issue.getReportedBy() != null) {
                this.reportedById = issue.getReportedBy().getId();
                this.reportedByName = issue.getReportedBy().getName();
                this.reportedByEmail = issue.getReportedBy().getEmail();
            }
        }
        if (history != null) {
            this.oldStatus = history.getOldStatus();
            this.newStatus = history.getNewStatus();
            this.remarks = history.getRemarks();
            this.changedAt = history.getChangedAt();
            if (history.getChangedBy() != null) {
                this.changedById = history.getChangedBy().getId();
                this.changedByName = history.getChangedBy().getName();
                this.changedByEmail = history.getChangedBy().getEmail();
            }
        }
        if (histories != null) {
            this.statusHistory = histories.stream().map(IssueStatusHistoryResponse::new).toList();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public IssueCategory getCategory() {
        return category;
    }

    public void setCategory(IssueCategory category) {
        this.category = category;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public IssuePriority getPriority() {
        return priority;
    }

    public void setPriority(IssuePriority priority) {
        this.priority = priority;
    }

    public IssueStatus getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(IssueStatus oldStatus) {
        this.oldStatus = oldStatus;
    }

    public IssueStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(IssueStatus newStatus) {
        this.newStatus = newStatus;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public void setStatus(IssueStatus status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Integer getChangedById() {
        return changedById;
    }

    public void setChangedById(Integer changedById) {
        this.changedById = changedById;
    }

    public String getChangedByName() {
        return changedByName;
    }

    public void setChangedByName(String changedByName) {
        this.changedByName = changedByName;
    }

    public String getChangedByEmail() {
        return changedByEmail;
    }

    public void setChangedByEmail(String changedByEmail) {
        this.changedByEmail = changedByEmail;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getReportedById() {
        return reportedById;
    }

    public void setReportedById(Integer reportedById) {
        this.reportedById = reportedById;
    }

    public String getReportedByName() {
        return reportedByName;
    }

    public void setReportedByName(String reportedByName) {
        this.reportedByName = reportedByName;
    }

    public String getReportedByEmail() {
        return reportedByEmail;
    }

    public void setReportedByEmail(String reportedByEmail) {
        this.reportedByEmail = reportedByEmail;
    }

    public List<IssueStatusHistoryResponse> getStatusHistory() {
        return statusHistory;
    }

    public void setStatusHistory(List<IssueStatusHistoryResponse> statusHistory) {
        this.statusHistory = statusHistory;
    }
}
