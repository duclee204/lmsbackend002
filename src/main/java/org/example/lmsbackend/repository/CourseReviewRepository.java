package org.example.lmsbackend.repository;

import org.example.lmsbackend.model.CourseReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseReviewRepository extends JpaRepository<CourseReview, Integer> {

    // Find all reviews for a specific course
    @Query("SELECT cr FROM CourseReview cr " +
           "JOIN FETCH cr.user u " +
           "JOIN FETCH cr.course c " +
           "WHERE c.courseId = :courseId " +
           "ORDER BY cr.createdAt DESC")
    List<CourseReview> findByCourseId(@Param("courseId") Integer courseId);

    // Find user's review for a specific course
    @Query("SELECT cr FROM CourseReview cr " +
           "JOIN FETCH cr.course c " +
           "WHERE cr.user.userId = :userId AND c.courseId = :courseId")
    Optional<CourseReview> findByUserIdAndCourseId(@Param("userId") Integer userId, @Param("courseId") Integer courseId);

    // Find all reviews by a specific user
    @Query("SELECT cr FROM CourseReview cr " +
           "JOIN FETCH cr.course c " +
           "WHERE cr.user.userId = :userId " +
           "ORDER BY cr.createdAt DESC")
    List<CourseReview> findByUserId(@Param("userId") Integer userId);

    // Find all reviews for courses taught by an instructor
    @Query("SELECT cr FROM CourseReview cr " +
           "JOIN FETCH cr.user u " +
           "JOIN FETCH cr.course c " +
           "WHERE c.instructorId = :instructorId " +
           "ORDER BY c.title ASC, cr.createdAt DESC")
    List<CourseReview> findByInstructorId(@Param("instructorId") Integer instructorId);

    // Get average rating for a course
    @Query("SELECT AVG(cr.rating) FROM CourseReview cr " +
           "JOIN cr.course c WHERE c.courseId = :courseId")
    Double getAverageRatingByCourseId(@Param("courseId") Integer courseId);

    // Count reviews for a course
    @Query("SELECT COUNT(cr) FROM CourseReview cr " +
           "JOIN cr.course c WHERE c.courseId = :courseId")
    Long countByCourseId(@Param("courseId") Integer courseId);
}
