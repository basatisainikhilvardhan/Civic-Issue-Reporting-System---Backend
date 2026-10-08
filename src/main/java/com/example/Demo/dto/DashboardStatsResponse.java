package com.example.Demo.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DashboardStatsResponse {

    private String status = "success";
    private String message = "Welcome to Admin Dashboard - Civic Issues Statistics";
    private boolean authorized = true;

    private long totalIssues;
    private long total;

    private long newIssues;
    private long assignedIssues;
    private long inProgressIssues;
    private long resolvedIssues;
    private long rejectedIssues;

    @JsonProperty("NEW")
    private long statusNew;

    @JsonProperty("ASSIGNED")
    private long statusAssigned;

    @JsonProperty("IN_PROGRESS")
    private long statusInProgress;

    @JsonProperty("RESOLVED")
    private long statusResolved;

    @JsonProperty("REJECTED")
    private long statusRejected;

    private Map<String, Long> statusBreakdown = new LinkedHashMap<>();

    @JsonCreator
    public DashboardStatsResponse() {
    }

    public DashboardStatsResponse(long totalIssues, long newIssues, long assignedIssues,
                                  long inProgressIssues, long resolvedIssues, long rejectedIssues) {
        this.status = "success";
        this.message = "Welcome to Admin Dashboard - Civic Issues Statistics";
        this.authorized = true;

        this.totalIssues = totalIssues;
        this.total = totalIssues;

        this.newIssues = newIssues;
        this.assignedIssues = assignedIssues;
        this.inProgressIssues = inProgressIssues;
        this.resolvedIssues = resolvedIssues;
        this.rejectedIssues = rejectedIssues;

        this.statusNew = newIssues;
        this.statusAssigned = assignedIssues;
        this.statusInProgress = inProgressIssues;
        this.statusResolved = resolvedIssues;
        this.statusRejected = rejectedIssues;

        this.statusBreakdown.put("NEW", newIssues);
        this.statusBreakdown.put("ASSIGNED", assignedIssues);
        this.statusBreakdown.put("IN_PROGRESS", inProgressIssues);
        this.statusBreakdown.put("RESOLVED", resolvedIssues);
        this.statusBreakdown.put("REJECTED", rejectedIssues);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public long getTotalIssues() {
        return totalIssues;
    }

    public void setTotalIssues(long totalIssues) {
        this.totalIssues = totalIssues;
        this.total = totalIssues;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
        this.totalIssues = total;
    }

    public long getNewIssues() {
        return newIssues;
    }

    public void setNewIssues(long newIssues) {
        this.newIssues = newIssues;
        this.statusNew = newIssues;
    }

    public long getAssignedIssues() {
        return assignedIssues;
    }

    public void setAssignedIssues(long assignedIssues) {
        this.assignedIssues = assignedIssues;
        this.statusAssigned = assignedIssues;
    }

    public long getInProgressIssues() {
        return inProgressIssues;
    }

    public void setInProgressIssues(long inProgressIssues) {
        this.inProgressIssues = inProgressIssues;
        this.statusInProgress = inProgressIssues;
    }

    public long getResolvedIssues() {
        return resolvedIssues;
    }

    public void setResolvedIssues(long resolvedIssues) {
        this.resolvedIssues = resolvedIssues;
        this.statusResolved = resolvedIssues;
    }

    public long getRejectedIssues() {
        return rejectedIssues;
    }

    public void setRejectedIssues(long rejectedIssues) {
        this.rejectedIssues = rejectedIssues;
        this.statusRejected = rejectedIssues;
    }

    public long getStatusNew() {
        return statusNew;
    }

    public void setStatusNew(long statusNew) {
        this.statusNew = statusNew;
        this.newIssues = statusNew;
    }

    public long getStatusAssigned() {
        return statusAssigned;
    }

    public void setStatusAssigned(long statusAssigned) {
        this.statusAssigned = statusAssigned;
        this.assignedIssues = statusAssigned;
    }

    public long getStatusInProgress() {
        return statusInProgress;
    }

    public void setStatusInProgress(long statusInProgress) {
        this.statusInProgress = statusInProgress;
        this.inProgressIssues = statusInProgress;
    }

    public long getStatusResolved() {
        return statusResolved;
    }

    public void setStatusResolved(long statusResolved) {
        this.statusResolved = statusResolved;
        this.resolvedIssues = statusResolved;
    }

    public long getStatusRejected() {
        return statusRejected;
    }

    public void setStatusRejected(long statusRejected) {
        this.statusRejected = statusRejected;
        this.rejectedIssues = statusRejected;
    }

    public Map<String, Long> getStatusBreakdown() {
        return statusBreakdown;
    }

    public void setStatusBreakdown(Map<String, Long> statusBreakdown) {
        this.statusBreakdown = statusBreakdown;
    }
}
