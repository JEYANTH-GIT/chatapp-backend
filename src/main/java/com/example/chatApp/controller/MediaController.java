package com.example.chatApp.media.controller;

import com.example.chatApp.auth.model.User;
import com.example.chatApp.media.dto.MediaUploadResponse;
import com.example.chatApp.media.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
@Tag(name = "Media & Notifications", description = "File upload, download, and media management endpoints")
public class MediaController {

    private final MediaService mediaService;

    @PersistenceContext
    private EntityManager entityManager;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload a file",
            description = "Upload a file or image. Supports all common file types up to 50MB."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully",
                    content = @Content(schema = @Schema(implementation = MediaUploadResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file or empty upload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> uploadFile(
            @Parameter(description = "File to upload", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Uploader user ID", required = true)
            @RequestParam("uploaderId") Long uploaderId) {
        try {
            User uploader = entityManager.find(User.class, uploaderId);
            if (uploader == null) {
                return ResponseEntity.badRequest().body("User not found with id: " + uploaderId);
            }
            MediaUploadResponse response = mediaService.uploadFile(file, uploader);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to upload file: " + e.getMessage());
        }
    }

    @GetMapping("/{mediaId}")
    @Operation(
            summary = "Get media metadata",
            description = "Retrieve metadata for a specific media file by ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Media metadata retrieved",
                    content = @Content(schema = @Schema(implementation = MediaUploadResponse.class))),
            @ApiResponse(responseCode = "404", description = "Media not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<?> getMediaById(
            @Parameter(description = "Media file ID", required = true)
            @PathVariable Long mediaId) {
        return mediaService.getMediaById(mediaId)
                .map(media -> ResponseEntity.ok((Object) media))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/file/{mediaId}")
    @Operation(
            summary = "Download/serve a file",
            description = "Download or serve a media file by its ID. Returns the actual file content."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File served successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Error reading file")
    })
    public ResponseEntity<?> serveFile(
            @Parameter(description = "Media file ID", required = true)
            @PathVariable Long mediaId) {
        try {
            Resource resource = mediaService.loadFileAsResource(mediaId);
            String contentType = mediaService.getContentType(mediaId);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{mediaId}")
    @Operation(
            summary = "Delete a media file",
            description = "Delete a media file. Only the uploader can delete their own files."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Not authorized to delete this file"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    public ResponseEntity<?> deleteMedia(
            @Parameter(description = "Media file ID", required = true)
            @PathVariable Long mediaId,
            @Parameter(description = "User ID of requester", required = true)
            @RequestParam("userId") Long userId) {
        try {
            mediaService.deleteMedia(mediaId, userId);
            return ResponseEntity.ok().body("File deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
