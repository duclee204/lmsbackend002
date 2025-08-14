package org.example.lmsbackend.dto;

import java.time.Instant;

public class CourseReviewDTO {
    private Integer reviewId;
    private Integer courseId;
    private String courseTitle;
    private String courseImage;
    private String description;
    private Integer userId;
    private String fullName;
    private String avatarUrl;
    private Integer rating;
    private String comment;
    private Instant createdAt;
    private boolean hasReviewed;
    private Double completionPercentage;

    // Constructors
    public CourseReviewDTO() {}

    public CourseReviewDTO(Integer courseId, String courseTitle) {
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.hasReviewed = false;
        this.completionPercentage = 0.0;
    }

    // Getters and Setters
    public Integer getReviewId() {
        return reviewId;
    }

    public void setReviewId(Integer reviewId) {
        this.reviewId = reviewId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public String getCourseImage() {
        return courseImage;
    }

    public void setCourseImage(String courseImage) {
        this.courseImage = courseImage;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isHasReviewed() {
        return hasReviewed;
    }

    public void setHasReviewed(boolean hasReviewed) {
        this.hasReviewed = hasReviewed;
    }

    public Double getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(Double completionPercentage) {
        this.completionPercentage = completionPercentage;
    }
}
