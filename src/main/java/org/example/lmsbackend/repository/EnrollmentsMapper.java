package org.example.lmsbackend.repository;

import org.apache.ibatis.annotations.*;
import org.example.lmsbackend.dto.EnrollmentsDTO;
import org.example.lmsbackend.dto.UserDTO;

import java.util.List;

@Mapper
public interface EnrollmentsMapper {
    @Select("SELECT course_id FROM enrollments WHERE user_id = #{userId}")
    List<Integer> getEnrolledCourseIdsByUserId(@Param("userId") int userId);

    @Select("SELECT COUNT(*) FROM enrollments WHERE user_id = #{userId} AND course_id = #{courseId}")
    int countEnrollment(@Param("userId") int userId, @Param("courseId") int courseId);

    @Insert("INSERT INTO enrollments(user_id, course_id) VALUES(#{userId}, #{courseId})")
    void enrollCourse(@Param("userId") int userId, @Param("courseId") int courseId);

    @Select("""
    SELECT 
        c.course_id AS courseId, 
        c.title AS courseTitle, 
        e.status, 
        e.enrolled_at AS enrolledAt
    FROM enrollments e
    JOIN courses c ON e.course_id = c.course_id
    WHERE e.user_id = #{userId}
""")
    List<EnrollmentsDTO> getEnrolledCoursesByUserId(@Param("userId") int userId);
    @Delete("""
    DELETE FROM enrollments 
    WHERE user_id = #{userId} AND course_id = #{courseId}
""")
    int deleteEnrollment(@Param("userId") int userId, @Param("courseId") int courseId);
    @Select("""
    SELECT 
        e.course_id AS courseId,
        c.title AS courseTitle,
        e.user_id AS userId,
        e.status,
        e.enrolled_at AS enrolledAt
    FROM enrollments e
    JOIN courses c ON e.course_id = c.course_id
""")
    List<EnrollmentsDTO> getAllEnrollments();

    @Select("""
    SELECT 
        u.user_id AS userId,
        u.username,
        u.password,
        u.email,
        u.full_name AS fullName,
        u.role,
        u.is_verified AS isVerified,
        u.cv_url AS cvUrl,
        u.avatar_url AS avatarUrl,
        u.verification_token AS verificationToken,
        u.verified_at AS verifiedAt,
        u.created_at AS createdAt,
        u.updated_at AS updatedAt
    FROM enrollments e
    JOIN users u ON e.user_id = u.user_id
    WHERE e.course_id = #{courseId}
""")
    List<UserDTO> getUsersByCourseId(@Param("courseId") int courseId);

}


