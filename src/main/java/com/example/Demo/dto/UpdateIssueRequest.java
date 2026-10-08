package com.example.Demo.dto;

public class UpdateIssueRequest {

    private String title;
    private String description;
    private String category;
    private String location;
    private String priority;
    private String imageUrl;

    public UpdateIssueRequest() {
    }

    public UpdateIssueRequest(String title, String description, String category, String location, String priority, String imageUrl) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.location = location;
        this.priority = priority;
        this.imageUrl = imageUrl;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
