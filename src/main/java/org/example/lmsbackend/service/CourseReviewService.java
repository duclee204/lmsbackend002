package org.example.lmsbackend.service;

import org.example.lmsbackend.dto.CourseCompletionDTO;
import org.example.lmsbackend.dto.CourseReviewDTO;
import org.example.lmsbackend.dto.ReviewRequestDTO;
import org.example.lmsbackend.model.Course;
import org.example.lmsbackend.model.CourseReview;
import org.example.lmsbackend.model.User;
import org.example.lmsbackend.repository.CourseRepository;
import org.example.lmsbackend.repository.CourseReviewRepository;
import org.example.lmsbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CourseReviewService {

    @Autowired
    private CourseReviewRepository courseReviewRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModulesService modulesService;

    private static final double MIN_COMPLETION_PERCENTAGE = 80.0;

    // Get eligible courses for review (completion >= 80%)
    public List<CourseReviewDTO> getEligibleCoursesForUser(Integer userId) {
        try {
            // Verify user exists
            userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get all published courses that user is enrolled in
            // For now, get all published courses (can be refined later with enrollment logic)
            List<Course> enrolledCourses = courseRepository.findAll()
                    .stream()
                    .filter(course -> course.getStatus() == Course.Status.published)
                    .toList();
            List<CourseReviewDTO> eligibleCourses = new ArrayList<>();

            for (Course course : enrolledCourses) {
                try {
                    // Calculate completion percentage using existing logic
                    CourseCompletionDTO completion = modulesService.calculateCourseCompletion(
                        course.getCourseId(), userId);

                    // Only include courses with >= 80% completion
                    if (completion.getCompletionPercentage() >= MIN_COMPLETION_PERCENTAGE) {
                        CourseReviewDTO dto = new CourseReviewDTO();
                        dto.setCourseId(course.getCourseId());
                        dto.setCourseTitle(course.getTitle());
                        dto.setDescription(course.getDescription());
                        dto.setCourseImage(course.getThumbnailUrl());
                        dto.setCompletionPercentage(completion.getCompletionPercentage());

                        // Check if user has already reviewed this course
                        Optional<CourseReview> existingReview = courseReviewRepository
                                .findByUserIdAndCourseId(userId, course.getCourseId());
                        dto.setHasReviewed(existingReview.isPresent());

                        eligibleCourses.add(dto);
                    }
                } catch (Exception e) {
                    System.err.println("Error calculating completion for course " + 
                        course.getCourseId() + ": " + e.getMessage());
                    // Skip this course if completion calculation fails
                    continue;
                }
            }

            System.out.println("📊 Found " + eligibleCourses.size() + 
                " courses eligible for review (>= " + MIN_COMPLETION_PERCENTAGE + "%)");

            return eligibleCourses;

        } catch (Exception e) {
            System.err.println("Error getting eligible courses for user " + userId + ": " + e.getMessage());
            throw new RuntimeException("Failed to get eligible courses", e);
        }
    }

    // Get reviews for a specific course
    public List<CourseReviewDTO> getReviewsByCourse(Integer courseId) {
        List<CourseReview> reviews = courseReviewRepository.findByCourseId(courseId);
        List<CourseReviewDTO> reviewDTOs = new ArrayList<>();

        for (CourseReview review : reviews) {
            CourseReviewDTO dto = convertToDTO(review);
            reviewDTOs.add(dto);
        }

        return reviewDTOs;
    }

    // Get user's review for a specific course
    public CourseReviewDTO getMyReview(Integer userId, Integer courseId) {
        Optional<CourseReview> review = courseReviewRepository.findByUserIdAndCourseId(userId, courseId);
        if (review.isPresent()) {
            return convertToDTO(review.get());
        }
        throw new RuntimeException("Review not found");
    }

    // Get all reviews for courses taught by an instructor
    public List<CourseReviewDTO> getInstructorCourseReviews(Integer instructorId) {
        List<CourseReview> reviews = courseReviewRepository.findByInstructorId(instructorId);
        List<CourseReviewDTO> reviewDTOs = new ArrayList<>();

        for (CourseReview review : reviews) {
            CourseReviewDTO dto = convertToDTO(review);
            reviewDTOs.add(dto);
        }

        return reviewDTOs;
    }

    // Get all reviews (admin only)
    public List<CourseReviewDTO> getAllReviews() {
        List<CourseReview> reviews = courseReviewRepository.findAll();
        List<CourseReviewDTO> reviewDTOs = new ArrayList<>();

        for (CourseReview review : reviews) {
            CourseReviewDTO dto = convertToDTO(review);
            reviewDTOs.add(dto);
        }

        return reviewDTOs;
    }

    // Create or update review
    @Transactional
    public CourseReviewDTO createOrUpdateReview(Integer userId, ReviewRequestDTO request) {
        try {
            // Validate rating
            if (request.getRating() < 1 || request.getRating() > 5) {
                throw new RuntimeException("Rating must be between 1 and 5");
            }

            // Check if user has sufficient completion percentage
            CourseCompletionDTO completion = modulesService.calculateCourseCompletion(
                request.getCourseId(), userId);
            
            if (completion.getCompletionPercentage() < MIN_COMPLETION_PERCENTAGE) {
                throw new RuntimeException("You need to complete at least " + MIN_COMPLETION_PERCENTAGE + 
                    "% of the course to review it. Current progress: " + 
                    String.format("%.1f", completion.getCompletionPercentage()) + "%");
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Course course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new RuntimeException("Course not found"));

            // Check for existing review
            Optional<CourseReview> existingReview = courseReviewRepository
                    .findByUserIdAndCourseId(userId, request.getCourseId());

            CourseReview review;
            if (existingReview.isPresent()) {
                // Update existing review
                review = existingReview.get();
                review.setRating(request.getRating());
                review.setComment(request.getComment());
                // Keep original created date for updates
            } else {
                // Create new review
                review = new CourseReview();
                review.setUser(user);
                review.setCourse(course);
                review.setRating(request.getRating());
                review.setComment(request.getComment());
                review.setCreatedAt(Instant.now());
            }

            CourseReview savedReview = courseReviewRepository.save(review);
            return convertToDTO(savedReview);

        } catch (Exception e) {
            System.err.println("Error creating/updating review: " + e.getMessage());
            throw new RuntimeException("Failed to save review", e);
        }
    }

    // Delete review (admin only)
    @Transactional
    public void deleteReview(Integer reviewId) {
        if (!courseReviewRepository.existsById(reviewId)) {
            throw new RuntimeException("Review not found");
        }
        courseReviewRepository.deleteById(reviewId);
    }

    // Helper method to convert entity to DTO
    private CourseReviewDTO convertToDTO(CourseReview review) {
        CourseReviewDTO dto = new CourseReviewDTO();
        dto.setReviewId(review.getId());
        dto.setCourseId(review.getCourse().getCourseId());
        dto.setCourseTitle(review.getCourse().getTitle());
        dto.setDescription(review.getCourse().getDescription());
        dto.setCourseImage(review.getCourse().getThumbnailUrl());
        dto.setUserId(review.getUser().getUserId());
        dto.setFullName(review.getUser().getFullName());
        dto.setAvatarUrl(review.getUser().getAvatarUrl());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setHasReviewed(true);
        return dto;
    }
}
