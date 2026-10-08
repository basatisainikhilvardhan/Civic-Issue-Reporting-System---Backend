package com.example.Demo.dto;

import com.example.Demo.Issue;
import com.example.Demo.IssueCategory;
import com.example.Demo.IssuePriority;
import com.example.Demo.IssueStatus;

public class IssueResponse {

    private Integer id;
    private String title;
    private String description;
    private IssueCategory category;
    private String location;
    private IssuePriority priority;
    private IssueStatus status;
    private String imageUrl;
    private Integer reportedById;
    private String reportedByName;
    private String reportedByEmail;
    private String createdAt;
    private String updatedAt;

    public IssueResponse() {
    }

    public IssueResponse(Issue issue) {
        if (issue != null) {
            this.id = issue.getId();
            this.title = issue.getTitle();
            this.description = issue.getDescription();
            this.category = issue.getCategory();
            this.location = issue.getLocation();
            this.priority = issue.getPriority();
            this.status = issue.getStatus();
            this.imageUrl = issue.getImageUrl();
            if (issue.getReportedBy() != null) {
                this.reportedById = issue.getReportedBy().getId();
                this.reportedByName = issue.getReportedBy().getName();
                this.reportedByEmail = issue.getReportedBy().getEmail();
            }
            this.createdAt = issue.getCreatedAt() != null ? issue.getCreatedAt().toString() : null;
            this.updatedAt = issue.getUpdatedAt() != null ? issue.getUpdatedAt().toString() : null;
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

    public IssueStatus getStatus() {
        return status;
    }

    public void setStatus(IssueStatus status) {
        this.status = status;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
