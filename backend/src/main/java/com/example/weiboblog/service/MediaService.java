package com.example.weiboblog.service;

import com.example.weiboblog.dto.MediaUploadResponse;
import com.example.weiboblog.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class MediaService {

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

    private final Path storageRoot;
    private final String publicBaseUrl;

    public MediaService(@Value("${app.media.storage-path:uploads}") String storagePath,
                        @Value("${app.media.base-url:/media/}") String publicBaseUrl) {
        this.storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();
        this.publicBaseUrl = normalizeBaseUrl(publicBaseUrl);
        try {
            Files.createDirectories(this.storageRoot);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create media storage directory", ex);
        }
    }

    public MediaUploadResponse store(MultipartFile file) {
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
        Path target = storageRoot.resolve(fileName);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("保存文件失败", ex);
        }
        String url = publicBaseUrl + fileName;
        String mediaType = MEDIA_KIND.getOrDefault(contentType, "unknown");
        return new MediaUploadResponse(url, contentType, mediaType, file.getSize(), file.getOriginalFilename());
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

    private String normalizeBaseUrl(String baseUrl) {
        String value = baseUrl == null ? "/media/" : baseUrl.trim();
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        if (!value.endsWith("/")) {
            value = value + "/";
        }
        return value;
    }

    public List<MediaUploadResponse> store(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("请选择要上传的文件");
        }
        if (files.size() > 4) {
            throw new BadRequestException("单次最多上传4个文件");
        }
        return files.stream()
                .map(this::store)
                .toList();
    }
}
