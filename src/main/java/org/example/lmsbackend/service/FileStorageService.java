package org.example.lmsbackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    @Autowired
    private CloudinaryService cloudinaryService;

    /**
     * Save file to Cloudinary
     * @param file - MultipartFile to upload
     * @param subFolder - folder name (avatars, cvs, etc.)
     * @return URL của file đã upload
     */
    public String saveFile(MultipartFile file, String subFolder) {
        try {
            // Determine resource type based on file type
            String resourceType = determineResourceType(file, subFolder);

            return cloudinaryService.uploadFile(file, subFolder, resourceType);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lưu file: " + e.getMessage(), e);
        }
    }

    /**
     * Determine Cloudinary resource type based on file and folder
     */
    private String determineResourceType(MultipartFile file, String subFolder) {
        String contentType = file.getContentType();

        // Video files
        if (contentType != null && contentType.startsWith("video/")) {
            return "video";
        }

        // Image files
        if (contentType != null && contentType.startsWith("image/")) {
            return "image";
        }

        // Documents and other files
        return "raw";
    }
}
