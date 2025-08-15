package org.example.lmsbackend.repository;

import org.apache.ibatis.annotations.*;
import org.example.lmsbackend.dto.PaymentDTO;
import org.example.lmsbackend.model.Payment;

import java.util.List;
import java.util.Optional;

@Mapper
public interface PaymentMapper {

    @Insert("INSERT INTO payments (user_id, course_id, amount, status, payment_method, transaction_id) " +
        "VALUES (#{userId}, #{courseId}, #{amount}, #{status}, #{paymentMethod}, #{transactionId})")
    @Options(useGeneratedKeys = true, keyProperty = "paymentId")
    int insertPayment(Payment payment);

    @Update("UPDATE payments SET status = #{status}, paid_at = #{paidAt} WHERE payment_id = #{paymentId}")
    int updatePaymentStatus(Payment payment);

    @Update("UPDATE payments SET status = #{status}, paid_at = NOW() WHERE transaction_id = #{transactionId}")
    int updatePaymentStatusByTransactionId(@Param("transactionId") String transactionId, @Param("status") String status);

    @Select("SELECT * FROM payments WHERE payment_id = #{paymentId}")
    @Results({
            @Result(property = "paymentId", column = "payment_id"),
            @Result(property = "user.userId", column = "user_id"),
            @Result(property = "course.courseId", column = "course_id"),
            @Result(property = "paymentMethod", column = "payment_method"),
            @Result(property = "transactionId", column = "transaction_id"),
            @Result(property = "paymentGatewayResponse", column = "payment_gateway_response"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "paidAt", column = "paid_at")
    })
    Optional<Payment> findPaymentById(Integer paymentId);

    @Select("SELECT p.payment_id, p.user_id, p.course_id, c.title as course_title, " +
            "p.amount, p.status, p.payment_method, p.transaction_id, p.created_at, p.paid_at " +
            "FROM payments p " +
            "JOIN courses c ON p.course_id = c.course_id " +
            "WHERE p.user_id = #{userId} " +
            "ORDER BY p.created_at DESC")
    @Results({
            @Result(property = "paymentId", column = "payment_id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "courseId", column = "course_id"),
            @Result(property = "courseTitle", column = "course_title"),
            @Result(property = "paymentMethod", column = "payment_method"),
            @Result(property = "transactionId", column = "transaction_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "paidAt", column = "paid_at")
    })
    List<PaymentDTO> getPaymentsByUserId(Integer userId);

    @Select("SELECT p.payment_id, p.user_id, p.course_id, c.title as course_title, " +
            "u.username as user_name, u.email as user_email, " +
            "p.amount, p.status, p.payment_method, p.transaction_id, p.created_at, p.paid_at " +
            "FROM payments p " +
            "JOIN courses c ON p.course_id = c.course_id " +
            "JOIN users u ON p.user_id = u.user_id " +
            "WHERE p.course_id = #{courseId} " +
            "ORDER BY p.created_at DESC")
    @Results({
            @Result(property = "paymentId", column = "payment_id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "courseId", column = "course_id"),
            @Result(property = "courseTitle", column = "course_title"),
            @Result(property = "userName", column = "user_name"),
            @Result(property = "userEmail", column = "user_email"),
            @Result(property = "paymentMethod", column = "payment_method"),
            @Result(property = "transactionId", column = "transaction_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "paidAt", column = "paid_at")
    })
    List<PaymentDTO> getPaymentsByCourseId(Integer courseId);

    @Select("SELECT p.payment_id, p.user_id, p.course_id, c.title as course_title, " +
            "u.username as user_name, u.email as user_email, " +
            "p.amount, p.status, p.payment_method, p.transaction_id, p.created_at, p.paid_at " +
            "FROM payments p " +
            "JOIN courses c ON p.course_id = c.course_id " +
            "JOIN users u ON p.user_id = u.user_id " +
            "ORDER BY p.created_at DESC")
    @Results({
            @Result(property = "paymentId", column = "payment_id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "courseId", column = "course_id"),
            @Result(property = "courseTitle", column = "course_title"),
            @Result(property = "userName", column = "user_name"),
            @Result(property = "userEmail", column = "user_email"),
            @Result(property = "paymentMethod", column = "payment_method"),
            @Result(property = "transactionId", column = "transaction_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "paidAt", column = "paid_at")
    })
    List<PaymentDTO> getAllPayments();

    @Select("SELECT * FROM payments WHERE transaction_id = #{transactionId}")
    @Results({
            @Result(property = "paymentId", column = "payment_id"),
            @Result(property = "user.userId", column = "user_id"),
            @Result(property = "course.courseId", column = "course_id"),
            @Result(property = "paymentMethod", column = "payment_method"),
            @Result(property = "transactionId", column = "transaction_id"),
            @Result(property = "paymentGatewayResponse", column = "payment_gateway_response"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "paidAt", column = "paid_at")
    })
    Optional<Payment> findPaymentByTransactionId(String transactionId);

    @Select("SELECT COUNT(*) FROM payments WHERE user_id = #{userId} AND course_id = #{courseId} AND status = 'completed'")
    int countCompletedPayments(Integer userId, Integer courseId);

    @Update("UPDATE payments SET vnpay_txn_ref = #{vnpayTxnRef}, updated_at = NOW() WHERE transaction_id = #{transactionId}")
    int updatePaymentVnpayTxnRef(@Param("transactionId") String transactionId, @Param("vnpayTxnRef") String vnpayTxnRef);

    @Select("SELECT * FROM payments WHERE course_id = #{courseId} AND user_id = #{userId}")
    @Results({
            @Result(property = "paymentId", column = "payment_id"),
            @Result(property = "user.userId", column = "user_id"),
            @Result(property = "course.courseId", column = "course_id"),
            @Result(property = "paymentMethod", column = "payment_method"),
            @Result(property = "transactionId", column = "transaction_id"),
            @Result(property = "vnpayTxnRef", column = "vnpay_txn_ref"),
            @Result(property = "paymentGatewayResponse", column = "payment_gateway_response"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "paidAt", column = "paid_at")
    })
    List<Payment> findByCourseIdAndUserId(@Param("courseId") Integer courseId, @Param("userId") Integer userId);

    @Select("SELECT * FROM payments WHERE vnpay_txn_ref = #{vnpayTxnRef}")
    @Results({
            @Result(property = "paymentId", column = "payment_id"),
            @Result(property = "user.userId", column = "user_id"),
            @Result(property = "course.courseId", column = "course_id"),
            @Result(property = "paymentMethod", column = "payment_method"),
            @Result(property = "transactionId", column = "transaction_id"),
            @Result(property = "vnpayTxnRef", column = "vnpay_txn_ref"),
            @Result(property = "paymentGatewayResponse", column = "payment_gateway_response"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "paidAt", column = "paid_at")
    })
    Payment findByVnpayTxnRef(@Param("vnpayTxnRef") String vnpayTxnRef);
}
