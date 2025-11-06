package com.example.weiboblog.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.example.weiboblog.config.AliyunOssProperties;
import com.example.weiboblog.dto.MediaUploadResponse;
import com.example.weiboblog.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class AliyunOssService {

    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024;
    private static final Map<String, String> MEDIA_KIND = Map.ofEntries(
            Map.entry("image/jpeg", "image"),
            Map.entry("image/jpg", "image"),
            Map.entry("image/png", "image"),
            Map.entry("image/gif", "image"),
            Map.entry("image/webp", "image"),
            Map.entry("video/mp4", "video"),
            Map.entry("video/webm", "video"),
            Map.entry("video/ogg", "video"),
            Map.entry("video/quicktime", "video"),
            Map.entry("video/x-m4v", "video")
    );
    private static final Set<String> ALLOWED_CONTENT_TYPES = MEDIA_KIND.keySet();
    private static final Map<String, String> FALLBACK_EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/png", "png",
            "image/gif", "gif",
            "image/webp", "webp",
            "video/mp4", "mp4",
            "video/webm", "webm",
            "video/ogg", "ogv",
            "video/quicktime", "mov",
            "video/x-m4v", "m4v"
    );

    private final OSS ossClient;
    private final AliyunOssProperties properties;

    public AliyunOssService(OSS ossClient, AliyunOssProperties properties) {
        this.ossClient = ossClient;
        this.properties = properties;
    }

    public MediaUploadResponse uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("请选择要上传的文件");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("文件过大，最大支持 50MB");
        }
        
        String contentType = determineContentType(file);
        validateContentType(contentType);

        String extension = resolveExtension(file, contentType);
        String fileName = buildFileName(extension);
        String objectKey = buildObjectKey(fileName);
        
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(file.getSize());
            
            PutObjectRequest putRequest = new PutObjectRequest(
                    properties.getBucketName(),
                    objectKey,
                    file.getInputStream(),
                    metadata
            );
            
            ossClient.putObject(putRequest);
            
            String url = buildFileUrl(objectKey);
            String mediaType = MEDIA_KIND.getOrDefault(contentType, "unknown");
            
            log.info("Successfully uploaded file to OSS: {}", objectKey);
            
            return new MediaUploadResponse(url, contentType, mediaType, file.getSize(), file.getOriginalFilename());
            
        } catch (IOException ex) {
            log.error("Failed to upload file to OSS", ex);
            throw new IllegalStateException("上传文件失败", ex);
        }
    }

    private String determineContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null) {
            return contentType.toLowerCase(Locale.ROOT);
        }
        String extension = StringUtils.getFilenameExtension(Objects.requireNonNullElse(file.getOriginalFilename(), ""));
        if (extension == null || extension.isBlank()) {
            throw new BadRequestException("无法识别文件类型");
        }
        return switch (extension.toLowerCase(Locale.ROOT)) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            case "ogv", "ogg" -> "video/ogg";
            case "mov", "qt" -> "video/quicktime";
            case "m4v" -> "video/x-m4v";
            default -> throw new BadRequestException("不支持的文件类型");
        };
    }

    private void validateContentType(String contentType) {
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("仅支持上传图片或视频");
        }
    }

    private String resolveExtension(MultipartFile file, String contentType) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String ext = StringUtils.getFilenameExtension(originalFilename);
            if (ext != null && !ext.isBlank()) {
                return ext.toLowerCase(Locale.ROOT);
            }
        }
        return FALLBACK_EXTENSIONS.getOrDefault(contentType, "bin");
    }

    private String buildFileName(String extension) {
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String random = UUID.randomUUID().toString().replaceAll("-", "");
        String sanitizedExt = extension.startsWith(".") ? extension.substring(1) : extension;
        return timestamp + "_" + random + "." + sanitizedExt;
    }

    private String buildObjectKey(String fileName) {
        return "uploads/" + fileName;
    }

    private String buildFileUrl(String objectKey) {
        if (StringUtils.hasText(properties.getDomain())) {
            return properties.getDomain().trim() + "/" + objectKey;
        }
        return "https://" + properties.getBucketName() + "." + properties.getEndpoint() + "/" + objectKey;
    }
}