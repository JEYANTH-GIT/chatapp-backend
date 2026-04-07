package com.example.chatApp.media.service;

import com.example.chatApp.auth.model.User;
import com.example.chatApp.media.dto.MediaUploadResponse;
import com.example.chatApp.media.model.MediaFile;
import com.example.chatApp.media.repository.MediaFileRepository;
import jakarta.annotation.PostConstruct;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MediaService {

    private final MediaFileRepository mediaFileRepository;
    private final Path uploadPath;

    public MediaService(MediaFileRepository mediaFileRepository,
                        @Value("${app.upload.dir:./uploads}") String uploadDir) {
        this.mediaFileRepository = mediaFileRepository;
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + uploadPath, e);
        }
    }

    public MediaUploadResponse uploadFile(MultipartFile file, User uploader) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file");
        }

        String originalFileName = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalFileName);
        String storedFileName = UUID.randomUUID().toString() + "." + extension;

        try {
            Path targetLocation = uploadPath.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            MediaFile mediaFile = MediaFile.builder()
                    .uploader(uploader)
                    .fileName(originalFileName)
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .storagePath(targetLocation.toString())
                    .url("/api/media/file/" + storedFileName)
                    .build();

            MediaFile saved = mediaFileRepository.save(mediaFile);
            return mapToResponse(saved);

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + originalFileName, e);
        }
    }

    public Optional<MediaUploadResponse> getMediaById(Long mediaId) {
        return mediaFileRepository.findById(mediaId).map(this::mapToResponse);
    }

    public Resource loadFileAsResource(Long mediaId) {
        MediaFile mediaFile = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media file not found with id: " + mediaId));

        try {
            Path filePath = Paths.get(mediaFile.getStoragePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found or not readable: " + mediaFile.getFileName());
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("File not found: " + mediaFile.getFileName(), e);
        }
    }

    public String getContentType(Long mediaId) {
        return mediaFileRepository.findById(mediaId)
                .map(MediaFile::getFileType)
                .orElse("application/octet-stream");
    }

    public List<MediaUploadResponse> getMediaByUploader(Long uploaderId) {
        return mediaFileRepository.findByUploaderId(uploaderId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public void deleteMedia(Long mediaId, Long userId) {
        MediaFile mediaFile = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media file not found with id: " + mediaId));

        if (!mediaFile.getUploader().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to delete this file");
        }

        try {
            Path filePath = Paths.get(mediaFile.getStoragePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log but don't fail — DB record will still be deleted
        }

        mediaFileRepository.delete(mediaFile);
    }

    private MediaUploadResponse mapToResponse(MediaFile mediaFile) {
        return MediaUploadResponse.builder()
                .id(mediaFile.getId())
                .fileName(mediaFile.getFileName())
                .fileType(mediaFile.getFileType())
                .fileSize(mediaFile.getFileSize())
                .url(mediaFile.getUrl())
                .uploaderId(mediaFile.getUploader().getId())
                .uploaderUsername(mediaFile.getUploader().getUsername())
                .uploadedAt(mediaFile.getUploadedAt())
                .build();
    }
}
