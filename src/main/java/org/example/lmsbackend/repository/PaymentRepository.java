package org.example.lmsbackend.repository;

import org.example.lmsbackend.model.Payment;
import org.example.lmsbackend.model.User;
import org.example.lmsbackend.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    // Tìm theo transaction ID
    Optional<Payment> findByTransactionId(String transactionId);

    // Tìm theo user và sắp xếp theo thời gian
    @Query("SELECT p FROM Payment p WHERE p.user.userId = :userId ORDER BY p.createdAt DESC")
    List<Payment> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    // Tìm theo course và user
    @Query("SELECT p FROM Payment p WHERE p.course.courseId = :courseId AND p.user.userId = :userId ORDER BY p.createdAt DESC")
    List<Payment> findByCourseIdAndUserIdOrderByCreatedAtDesc(@Param("courseId") Long courseId, @Param("userId") Long userId);

    // Lấy 10 payment gần nhất
    @Query("SELECT p FROM Payment p ORDER BY p.createdAt DESC")
    List<Payment> findTop10ByOrderByCreatedAtDesc();

    // Tìm payment thành công cho course và user
    @Query("SELECT p FROM Payment p WHERE p.course.courseId = :courseId AND p.user.userId = :userId AND p.status = 'completed' ORDER BY p.createdAt DESC")
    List<Payment> findSuccessfulPaymentForCourse(@Param("courseId") Long courseId, @Param("userId") Long userId);

    // Kiểm tra user đã thanh toán course chưa
    @Query("SELECT COUNT(p) > 0 FROM Payment p WHERE p.course.courseId = :courseId AND p.user.userId = :userId AND p.status = 'completed'")
    boolean existsSuccessfulPaymentForCourse(@Param("courseId") Long courseId, @Param("userId") Long userId);

    // Tìm theo status
    List<Payment> findByStatus(String status);
}
