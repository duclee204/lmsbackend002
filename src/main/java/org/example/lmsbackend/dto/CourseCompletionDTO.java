package org.example.lmsbackend.dto;

public class CourseCompletionDTO {
    private Integer courseId;
    private String courseTitle;
    private Double completionPercentage;
    private Integer totalModules;
    private Integer completedModules;
    private Integer totalItems;
    private Integer completedItems;
    private Integer totalContents;
    private Integer completedContents;
    private Integer totalVideos;
    private Integer completedVideos;
    private Integer totalQuizzes;
    private Integer completedQuizzes;

    // Constructors
    public CourseCompletionDTO() {}

    public CourseCompletionDTO(Integer courseId, String courseTitle) {
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.completionPercentage = 0.0;
        this.totalModules = 0;
        this.completedModules = 0;
        this.totalItems = 0;
        this.completedItems = 0;
        this.totalContents = 0;
        this.completedContents = 0;
        this.totalVideos = 0;
        this.completedVideos = 0;
        this.totalQuizzes = 0;
        this.completedQuizzes = 0;
    }

    // Getters and Setters
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

    public Double getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(Double completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    public Integer getTotalModules() {
        return totalModules;
    }

    public void setTotalModules(Integer totalModules) {
        this.totalModules = totalModules;
    }

    public Integer getCompletedModules() {
        return completedModules;
    }

    public void setCompletedModules(Integer completedModules) {
        this.completedModules = completedModules;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public Integer getCompletedItems() {
        return completedItems;
    }

    public void setCompletedItems(Integer completedItems) {
        this.completedItems = completedItems;
    }

    public Integer getTotalContents() {
        return totalContents;
    }

    public void setTotalContents(Integer totalContents) {
        this.totalContents = totalContents;
    }

    public Integer getCompletedContents() {
        return completedContents;
    }

    public void setCompletedContents(Integer completedContents) {
        this.completedContents = completedContents;
    }

    public Integer getTotalVideos() {
        return totalVideos;
    }

    public void setTotalVideos(Integer totalVideos) {
        this.totalVideos = totalVideos;
    }

    public Integer getCompletedVideos() {
        return completedVideos;
    }

    public void setCompletedVideos(Integer completedVideos) {
        this.completedVideos = completedVideos;
    }

    public Integer getTotalQuizzes() {
        return totalQuizzes;
    }

    public void setTotalQuizzes(Integer totalQuizzes) {
        this.totalQuizzes = totalQuizzes;
    }

    public Integer getCompletedQuizzes() {
        return completedQuizzes;
    }

    public void setCompletedQuizzes(Integer completedQuizzes) {
        this.completedQuizzes = completedQuizzes;
    }
}
