package org.example.lmsbackend.service;

import org.example.lmsbackend.dto.ModulesDTO;
import org.example.lmsbackend.dto.CourseCompletionDTO;
import org.example.lmsbackend.model.Course;
import org.example.lmsbackend.model.Modules;
import org.example.lmsbackend.model.User;
import org.example.lmsbackend.model.Video;
import org.example.lmsbackend.model.Quizzes;
import org.example.lmsbackend.repository.CourseRepository;
import org.example.lmsbackend.repository.ModulesRepository;
import org.example.lmsbackend.repository.UserMapper;
import org.example.lmsbackend.repository.VideoMapper;
import org.example.lmsbackend.repository.ContentRepository;
import org.example.lmsbackend.repository.VideoRepository;
import org.example.lmsbackend.repository.QuizzesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import org.example.lmsbackend.model.Content;

@Service
public class ModulesService {
    @Autowired private CourseRepository courseRepository;
    @Autowired private ModulesRepository modulesRepository;
    @Autowired private UserMapper userMapper;
    @Autowired private VideoMapper videoMapper;
    @Autowired private QuizzesService quizzesService;
    @Autowired private ContentRepository contentRepository;
    @Autowired private VideoRepository videoRepository;
    @Autowired private QuizzesRepository quizzesRepository;
    @Autowired private ModuleProgressService moduleProgressService;

    public List<Modules> getModulesByCourseId(int courseId) {
        return modulesRepository.findByCourse_CourseIdOrderByOrderNumber(courseId);
    }

    // ✅ STUDENT API: Lấy danh sách module đã xuất bản theo courseId
    public List<Modules> getPublishedModulesByCourseId(int courseId) {
        return modulesRepository.findByCourse_CourseIdAndPublishedTrueOrderByOrderNumber(courseId);
    }

    public Modules createModule(String username, ModulesDTO dto) {
        User user = userMapper.findByUsername(username);
        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (!course.getInstructor().getUserId().equals(user.getUserId())) {
            throw new RuntimeException("You are not the instructor of this course.");
        }

        // Kiểm tra trùng orderNumber khi tạo module mới
        if (isOrderNumberExistsForNewModule(dto.getCourseId(), dto.getOrderNumber())) {
            throw new RuntimeException("Số thứ tự " + dto.getOrderNumber() + " đã tồn tại trong khóa học này. Vui lòng chọn số khác!");
        }

        Modules module = new Modules();
        module.setCourse(course);
        module.setTitle(dto.getTitle());
        module.setDescription(dto.getDescription());
        module.setOrderNumber(dto.getOrderNumber());

        // ✅ SỬ DỤNG GIÁ TRỊ TỪ DTO, NẾU KHÔNG CÓ THÌ MẶC ĐỊNH LÀ false
        module.setPublished(dto.isPublished() != null ? dto.isPublished() : false);

        return modulesRepository.save(module);
    }

    public Modules getModuleById(int moduleId) {
        return modulesRepository.findById(moduleId)
                .orElseThrow(() -> new RuntimeException("Module not found"));
    }
    public Modules updateModuleStatus(int moduleId, boolean published, String username) {
        Modules module = getModuleById(moduleId);
        if (!module.getCourse().getInstructor().getUsername().equals(username)) {
            throw new RuntimeException("Not authorized to update this module");
        }

        System.out.println("=== Updating Module Status ===");
        System.out.println("Module ID: " + moduleId);
        System.out.println("New Status: " + (published ? "Published" : "Not Published"));
        System.out.println("Content count: " + module.getContents().size());

        module.setPublished(published);

        // Cập nhật tất cả content trong module theo trạng thái module
        int updatedContentCount = 0;
        for (Content content : module.getContents()) {
            boolean oldStatus = content.isPublished();
            content.setPublished(published);
            
            if (oldStatus != published) {
                updatedContentCount++;
                System.out.println("Content '" + content.getTitle() + "' status changed from " + 
                    oldStatus + " to " + published);
            }
        }

        // Cập nhật tất cả video trong module theo trạng thái module
        int updatedVideoCount = 0;
        try {
            List<Video> videos = videoMapper.findVideosByModuleId(moduleId);
            for (Video video : videos) {
                boolean oldStatus = video.getPublished() != null ? video.getPublished() : false;
                if (oldStatus != published) {
                    video.setPublished(published);
                    videoMapper.updateVideo(video);
                    updatedVideoCount++;
                    System.out.println("Video '" + video.getTitle() + "' status changed from " +
                        oldStatus + " to " + published);
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating videos: " + e.getMessage());
        }

        // Cập nhật tất cả quiz trong module theo trạng thái module
        int updatedQuizCount = 0;
        try {
            // Sử dụng QuizzesService để lấy và cập nhật quiz
            quizzesService.updateQuizzesByModuleStatus(moduleId, published);
            updatedQuizCount = quizzesService.getQuizzesByModule(moduleId, null).size();
            System.out.println("Updated " + updatedQuizCount + " quizzes in module " + moduleId);
        } catch (Exception e) {
            System.err.println("Error updating quizzes: " + e.getMessage());
        }

        System.out.println("Updated " + updatedContentCount + " content items, " +
                          updatedVideoCount + " videos, " + updatedQuizCount + " quizzes");

        Modules savedModule = modulesRepository.save(module); // cascade sẽ lưu cả content nếu có @OneToMany(cascade = ...)
        
        System.out.println("✅ Module and all content/video/quiz status updated successfully");
        return savedModule;
    }
    // ✅ Cập nhật module
    public Modules updateModule(int moduleId, ModulesDTO dto, String username) {
        Modules module = getModuleById(moduleId);
        
        // Kiểm tra quyền
        if (!module.getCourse().getInstructor().getUsername().equals(username)) {
            throw new RuntimeException("Not authorized to update this module");
        }
        
        // Kiểm tra trùng orderNumber
        if (isOrderNumberExists(module.getCourse().getCourseId(), dto.getOrderNumber(), moduleId)) {
            throw new RuntimeException("Số thứ tự " + dto.getOrderNumber() + " đã tồn tại trong khóa học này. Vui lòng chọn số khác!");
        }
        
        // Check if status is changing
        boolean statusChanged = module.isPublished() != dto.isPublished();
        
        // Cập nhật thông tin
        module.setTitle(dto.getTitle());
        module.setDescription(dto.getDescription());
        module.setOrderNumber(dto.getOrderNumber());
        module.setPublished(dto.isPublished());
        
        // If status changed, update all content in module
        if (statusChanged) {
            System.out.println("=== Module status changed in update, updating content ===");
            System.out.println("New status: " + (dto.isPublished() ? "Published" : "Not Published"));
            
            int updatedContentCount = 0;
            for (Content content : module.getContents()) {
                content.setPublished(dto.isPublished());
                updatedContentCount++;
            }
            
            System.out.println("Updated " + updatedContentCount + " content items to match module status");
        }
        
        return modulesRepository.save(module);
    }

    // ✅ Xóa module
    public void deleteModule(int moduleId) {
        Modules module = getModuleById(moduleId);
        modulesRepository.delete(module);
    }

    public void ensureInstructorOwnsCourse(int courseId, String username) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        User user = userMapper.findByUsername(username);
        if (user == null || !course.getInstructor().getUserId().equals(user.getUserId())) {
            throw new RuntimeException("Bạn không phải giảng viên của khóa học này.");
        }
    }

    // ✅ Kiểm tra trùng orderNumber trong course (trừ module hiện tại)
    private boolean isOrderNumberExists(int courseId, int orderNumber, int excludeModuleId) {
        List<Modules> modules = modulesRepository.findByCourse_CourseIdOrderByOrderNumber(courseId);
        return modules.stream()
                .filter(m -> m.getId() != excludeModuleId) // Loại trừ module hiện tại
                .anyMatch(m -> m.getOrderNumber() == orderNumber);
    }

    // ✅ Kiểm tra trùng orderNumber khi tạo module mới
    private boolean isOrderNumberExistsForNewModule(int courseId, int orderNumber) {
        List<Modules> modules = modulesRepository.findByCourse_CourseIdOrderByOrderNumber(courseId);
        return modules.stream()
                .anyMatch(m -> m.getOrderNumber() == orderNumber);
    }

    public void ensureInstructorOwnsModule(int moduleId, String username) {
        Modules module = getModuleById(moduleId);
        if (!module.getCourse().getInstructor().getUsername().equals(username)) {
            throw new RuntimeException("Bạn không phải giảng viên của module này.");
        }
    }

    // ✅ Tính % hoàn thành khóa học cho user
    public CourseCompletionDTO calculateCourseCompletion(Integer courseId, Integer userId) {
        try {
            // Get course info
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Course not found"));

            CourseCompletionDTO dto = new CourseCompletionDTO(courseId, course.getTitle());

            // Get all published modules in course
            List<Modules> publishedModules = modulesRepository.findByCourse_CourseIdAndPublishedTrueOrderByOrderNumber(courseId);
            dto.setTotalModules(publishedModules.size());

            int totalItems = 0;
            int completedItems = 0;
            int totalContents = 0, completedContents = 0;
            int totalVideos = 0, completedVideos = 0;
            int totalQuizzes = 0, completedQuizzes = 0;
            int completedModules = 0;

            // Loop through each module and calculate completion
            for (Modules module : publishedModules) {
                boolean moduleCompleted = true;

                // Count content items
                List<Content> moduleContents = contentRepository.findByModuleIdAndPublishedTrueOrderByOrderNumber(module.getId());
                int moduleContentTotal = moduleContents.size();
                int moduleContentCompleted = 0;

                for (Content content : moduleContents) {
                    if (moduleProgressService.hasCompletedSpecificContent(userId, content.getId())) {
                        moduleContentCompleted++;
                    }
                }

                totalContents += moduleContentTotal;
                completedContents += moduleContentCompleted;

                if (moduleContentTotal > 0 && moduleContentCompleted < moduleContentTotal) {
                    moduleCompleted = false;
                }

                // Count video items
                List<org.example.lmsbackend.model.Video> moduleVideos = videoRepository.findByModuleIdAndPublished(module.getId());
                int moduleVideoTotal = moduleVideos.size();
                int moduleVideoCompleted = 0;

                for (org.example.lmsbackend.model.Video video : moduleVideos) {
                    if (moduleProgressService.hasCompletedSpecificVideo(userId, video.getVideoId().intValue())) {
                        moduleVideoCompleted++;
                    }
                }

                totalVideos += moduleVideoTotal;
                completedVideos += moduleVideoCompleted;

                if (moduleVideoTotal > 0 && moduleVideoCompleted < moduleVideoTotal) {
                    moduleCompleted = false;
                }

                // Count quiz items
                List<Quizzes> moduleQuizzes = quizzesRepository.findByModuleIdAndPublishTrue(module.getId());
                int moduleQuizTotal = moduleQuizzes.size();
                int moduleQuizCompleted = 0;

                for (Quizzes quiz : moduleQuizzes) {
                    if (moduleProgressService.hasCompletedSpecificQuiz(userId, quiz.getQuizId())) {
                        moduleQuizCompleted++;
                    }
                }

                totalQuizzes += moduleQuizTotal;
                completedQuizzes += moduleQuizCompleted;

                if (moduleQuizTotal > 0 && moduleQuizCompleted < moduleQuizTotal) {
                    moduleCompleted = false;
                }

                // If all items in module are completed
                if (moduleCompleted) {
                    completedModules++;
                }
            }

            // Calculate totals
            totalItems = totalContents + totalVideos + totalQuizzes;
            completedItems = completedContents + completedVideos + completedQuizzes;

            // Set results
            dto.setCompletedModules(completedModules);
            dto.setTotalItems(totalItems);
            dto.setCompletedItems(completedItems);
            dto.setTotalContents(totalContents);
            dto.setCompletedContents(completedContents);
            dto.setTotalVideos(totalVideos);
            dto.setCompletedVideos(completedVideos);
            dto.setTotalQuizzes(totalQuizzes);
            dto.setCompletedQuizzes(completedQuizzes);

            // Calculate completion percentage
            double completionPercentage = totalItems > 0 ? (double) completedItems * 100.0 / totalItems : 0.0;
            dto.setCompletionPercentage(completionPercentage);

            System.out.println("📊 Course " + courseId + " completion for user " + userId + ": " +
                String.format("%.1f", completionPercentage) + "% (" + completedItems + "/" + totalItems + ")");

            return dto;
        } catch (Exception e) {
            System.err.println("❌ Error calculating course completion: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error calculating course completion: " + e.getMessage());
        }
    }
}
