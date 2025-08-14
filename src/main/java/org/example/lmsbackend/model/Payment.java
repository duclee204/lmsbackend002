package org.example.lmsbackend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
@Getter
@Setter
public class Payment {

    public enum Status {
        pending, completed, failed, refunded
    }

    public enum PaymentMethod {
        credit_card, bank_transfer, momo, zalopay, vnpay
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Integer paymentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.pending;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "transaction_id", length = 255)
    private String transactionId;

    @Column(name = "vnpay_txn_ref", length = 100)
    private String vnpayTxnRef;

    @Column(name = "payment_gateway_response", columnDefinition = "TEXT")
    private String paymentGatewayResponse;

    @Column(name = "created_at", insertable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant updatedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        if (status == Status.completed && paidAt == null) {
            paidAt = Instant.now();
        }
    }

    public Integer getUserId() {
        return user != null ? user.getUserId() : null;
    }

    public Integer getCourseId() {
        return course != null ? course.getCourseId() : null;
    }
}
