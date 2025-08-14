package org.example.lmsbackend.controller;

import org.example.lmsbackend.dto.CourseReviewDTO;
import org.example.lmsbackend.dto.ReviewRequestDTO;
import org.example.lmsbackend.service.CourseReviewService;
import org.example.lmsbackend.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/course-reviews")
public class CourseReviewController {

    @Autowired
    private CourseReviewService courseReviewService;

    // Get eligible courses for review (student only, completion >= 80%)
    @GetMapping("/eligible")
    @PreAuthorize("hasRole('student')")
    public ResponseEntity<List<CourseReviewDTO>> getEligibleCourses() {
        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer userId = userDetails.getUserId();

            List<CourseReviewDTO> eligibleCourses = courseReviewService.getEligibleCoursesForUser(userId);

            return ResponseEntity.ok(eligibleCourses);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Get reviews for a specific course
    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasRole('student') or hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<List<CourseReviewDTO>> getReviewsByCourse(@PathVariable Integer courseId) {
        try {
            List<CourseReviewDTO> reviews = courseReviewService.getReviewsByCourse(courseId);
            return ResponseEntity.ok(reviews);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Get my review for a specific course
    @GetMapping("/my-review/{courseId}")
    @PreAuthorize("hasRole('student')")
    public ResponseEntity<CourseReviewDTO> getMyReview(@PathVariable Integer courseId) {
        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer userId = userDetails.getUserId();

            CourseReviewDTO review = courseReviewService.getMyReview(userId, courseId);
            return ResponseEntity.ok(review);

        } catch (Exception e) {
            // Return 404 if review not found
            return ResponseEntity.notFound().build();
        }
    }

    // Get all reviews for courses taught by instructor
    @GetMapping("/instructor")
    @PreAuthorize("hasRole('instructor')")
    public ResponseEntity<List<CourseReviewDTO>> getInstructorCourseReviews() {
        try {
            // Get current instructor
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer instructorId = userDetails.getUserId();

            List<CourseReviewDTO> reviews = courseReviewService.getInstructorCourseReviews(instructorId);
            return ResponseEntity.ok(reviews);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Get all reviews (admin only)
    @GetMapping("/all")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<List<CourseReviewDTO>> getAllReviews() {
        try {
            List<CourseReviewDTO> reviews = courseReviewService.getAllReviews();
            return ResponseEntity.ok(reviews);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Create or update review
    @PostMapping
    @PreAuthorize("hasRole('student')")
    public ResponseEntity<?> createOrUpdateReview(@RequestBody ReviewRequestDTO request) {
        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer userId = userDetails.getUserId();

            CourseReviewDTO review = courseReviewService.createOrUpdateReview(userId, request);

            return ResponseEntity.ok(review);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Delete review (admin only)
    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> deleteReview(@PathVariable Integer reviewId) {
        try {
            courseReviewService.deleteReview(reviewId);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
}
