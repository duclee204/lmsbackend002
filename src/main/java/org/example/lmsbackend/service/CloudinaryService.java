package org.example.lmsbackend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    /**
     * Upload file to Cloudinary
     * @param file - MultipartFile to upload
     * @param folder - folder name in Cloudinary (avatars, videos, documents, etc.)
     * @param resourceType - "image", "video", "raw" (for documents)
     * @return URL của file đã upload
     */
    public String uploadFile(MultipartFile file, String folder, String resourceType) {
        try {
            // Generate unique public_id
            String originalFilename = file.getOriginalFilename();
            String filename = originalFilename != null ? 
                originalFilename.substring(0, originalFilename.lastIndexOf('.')) : "file";
            String publicId = folder + "/" + UUID.randomUUID().toString() + "_" + filename;

            // Upload options
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadOptions = ObjectUtils.asMap(
                "public_id", publicId,
                "resource_type", resourceType,
                "folder", folder
            );

            // Special options for videos
            if ("video".equals(resourceType)) {
                uploadOptions.put("quality", "auto");
                uploadOptions.put("format", "mp4");
                
                // For large videos, use simplified approach
                long fileSizeInMB = file.getSize() / (1024 * 1024);
                if (fileSizeInMB > 10) { // Videos larger than 10MB
                    // Remove transformations that might cause issues, keep it simple
                    uploadOptions.remove("quality");
                    uploadOptions.remove("format");
                    System.out.println("Uploading large video: " + fileSizeInMB + "MB (simplified mode)");
                }
            }

            // Upload to Cloudinary
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadOptions);
            
            return uploadResult.get("secure_url").toString();
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to Cloudinary: " + e.getMessage(), e);
        }
    }

    /**
     * Upload image (avatar, course thumbnail)
     */
    public String uploadImage(MultipartFile file, String folder) {
        return uploadFile(file, folder, "image");
    }

    /**
     * Upload video
     */
    public String uploadVideo(MultipartFile file, String folder) {
        return uploadFile(file, folder, "video");
    }

    /**
     * Upload document (PDF, DOC, etc.)
     */
    public String uploadDocument(MultipartFile file, String folder) {
        return uploadFile(file, folder, "raw");
    }

    /**
     * Delete file from Cloudinary
     */
    public boolean deleteFile(String publicId, String resourceType) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> deleteOptions = ObjectUtils.asMap("resource_type", resourceType);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().destroy(publicId, deleteOptions);
            return "ok".equals(result.get("result"));
        } catch (IOException e) {
            System.err.println("Failed to delete file from Cloudinary: " + e.getMessage());
            return false;
        }
    }

    /**
     * Extract public_id from Cloudinary URL
     */
    public String extractPublicId(String cloudinaryUrl) {
        if (cloudinaryUrl == null || !cloudinaryUrl.contains("cloudinary.com")) {
            return null;
        }
        
        try {
            // URL format: https://res.cloudinary.com/{cloud_name}/{resource_type}/upload/v{version}/{public_id}.{format}
            String[] parts = cloudinaryUrl.split("/");
            String fileNameWithExtension = parts[parts.length - 1];
            String fileName = fileNameWithExtension.contains(".") ? 
                fileNameWithExtension.substring(0, fileNameWithExtension.lastIndexOf('.')) : 
                fileNameWithExtension;
            
            // Find folder path
            for (int i = 0; i < parts.length; i++) {
                if ("upload".equals(parts[i]) && i + 2 < parts.length) {
                    // Skip version (v{number})
                    StringBuilder publicId = new StringBuilder();
                    for (int j = i + 2; j < parts.length - 1; j++) {
                        if (publicId.length() > 0) {
                            publicId.append("/");
                        }
                        publicId.append(parts[j]);
                    }
                    publicId.append("/").append(fileName);
                    return publicId.toString();
                }
            }
            
            return fileName;
        } catch (Exception e) {
            System.err.println("Failed to extract public_id from URL: " + cloudinaryUrl);
            return null;
        }
    }
}
