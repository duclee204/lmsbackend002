package org.example.lmsbackend.dto;

public class PaymentResponse {
    private boolean success;
    private String message;
    private Integer paymentId;
    private String paymentUrl; // URL thanh toán từ cổng thanh toán (nếu có)
    private String transactionId;
    private Long coursePrice;
    private Integer courseId;

    public PaymentResponse() {}

    public PaymentResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public PaymentResponse(boolean success, String message, Integer paymentId,
                           String paymentUrl, String transactionId) {
        this.success = success;
        this.message = message;
        this.paymentId = paymentId;
        this.paymentUrl = paymentUrl;
        this.transactionId = transactionId;
    }

    public PaymentResponse(boolean success, String message, Integer paymentId,
                           String paymentUrl, String transactionId, Long coursePrice, Integer courseId) {
        this.success = success;
        this.message = message;
        this.paymentId = paymentId;
        this.paymentUrl = paymentUrl;
        this.transactionId = transactionId;
        this.coursePrice = coursePrice;
        this.courseId = courseId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Integer paymentId) {
        this.paymentId = paymentId;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Long getCoursePrice() {
        return coursePrice;
    }

    public void setCoursePrice(Long coursePrice) {
        this.coursePrice = coursePrice;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }
}
