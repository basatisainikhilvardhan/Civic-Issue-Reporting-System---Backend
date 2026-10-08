package com.example.Demo.dto;

import com.example.Demo.IssueStatus;
import com.example.Demo.IssueStatusHistory;
import java.time.LocalDateTime;

public class IssueStatusHistoryResponse {

    private Integer id;
    private Integer issueId;
    private IssueStatus oldStatus;
    private IssueStatus newStatus;
    private String remarks;
    private Integer changedById;
    private String changedByName;
    private String changedByEmail;
    private LocalDateTime changedAt;

    public IssueStatusHistoryResponse() {
    }

    public IssueStatusHistoryResponse(IssueStatusHistory history) {
        if (history != null) {
            this.id = history.getId();
            if (history.getIssue() != null) {
                this.issueId = history.getIssue().getId();
            }
            this.oldStatus = history.getOldStatus();
            this.newStatus = history.getNewStatus();
            this.remarks = history.getRemarks();
            if (history.getChangedBy() != null) {
                this.changedById = history.getChangedBy().getId();
                this.changedByName = history.getChangedBy().getName();
                this.changedByEmail = history.getChangedBy().getEmail();
            }
            this.changedAt = history.getChangedAt();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIssueId() {
        return issueId;
    }

    public void setIssueId(Integer issueId) {
        this.issueId = issueId;
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
}
