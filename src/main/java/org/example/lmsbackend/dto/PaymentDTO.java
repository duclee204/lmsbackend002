package org.example.lmsbackend.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentDTO {
    private Integer paymentId;
    private Integer userId;
    private Integer courseId;
    private String courseTitle;
    private BigDecimal amount;
    private String status;
    private String paymentMethod;
    private String transactionId;
    private Instant createdAt;
    private Instant paidAt;

    public PaymentDTO() {}

    public PaymentDTO(Integer paymentId, Integer userId, Integer courseId, String courseTitle,
                      BigDecimal amount, String status, String paymentMethod,
                      String transactionId, Instant createdAt, Instant paidAt) {
        this.paymentId = paymentId;
        this.userId = userId;
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.amount = amount;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.transactionId = transactionId;
        this.createdAt = createdAt;
        this.paidAt = paidAt;
    }

    // Getters and Setters
    public Integer getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Integer paymentId) {
        this.paymentId = paymentId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }
}
