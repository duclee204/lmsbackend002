package org.example.lmsbackend.dto;

import java.math.BigDecimal;

public class PaymentRequest {
    private Integer courseId;
    private String paymentMethod; // credit_card, bank_transfer, momo, zalopay, vnpay
    private BigDecimal amount;

    public PaymentRequest() {}

    public PaymentRequest(Integer courseId, String paymentMethod, BigDecimal amount) {
        this.courseId = courseId;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
