package org.example.lmsbackend.dto;

public class EnrollmentsDTO {
    private int courseId;
    private String courseTitle;
    private int userId; // 👈 Thêm trường này
    private String status;
    private String enrolledAt;

    public EnrollmentsDTO() {}

    public EnrollmentsDTO(int courseId, String courseTitle, int userId, String status, String enrolledAt) {
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.userId = userId;
        this.status = status;
        this.enrolledAt = enrolledAt;
    }

    // Getters and setters
    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(String enrolledAt) {
        this.enrolledAt = enrolledAt;
    }
}

