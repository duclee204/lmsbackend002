package org.example.lmsbackend.dto;

public class ZaloPayRequest {
    private Integer courseId;
    private Long amount;
    private String orderInfo;
    private String description;

    // Constructors
    public ZaloPayRequest() {}

    public ZaloPayRequest(Integer courseId, Long amount, String orderInfo, String description) {
        this.courseId = courseId;
        this.amount = amount;
        this.orderInfo = orderInfo;
        this.description = description;
    }

    // Getters and Setters
    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getOrderInfo() {
        return orderInfo;
    }

    public void setOrderInfo(String orderInfo) {
        this.orderInfo = orderInfo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "ZaloPayRequest{" +
                "courseId=" + courseId +
                ", amount=" + amount +
                ", orderInfo='" + orderInfo + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
