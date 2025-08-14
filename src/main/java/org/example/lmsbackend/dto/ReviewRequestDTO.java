package org.example.lmsbackend.dto;

public class ReviewRequestDTO {
    private Integer courseId;
    private Integer rating;
    private String comment;

    // Constructors
    public ReviewRequestDTO() {}

    public ReviewRequestDTO(Integer courseId, Integer rating, String comment) {
        this.courseId = courseId;
        this.rating = rating;
        this.comment = comment;
    }

    // Getters and Setters
    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
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
}
