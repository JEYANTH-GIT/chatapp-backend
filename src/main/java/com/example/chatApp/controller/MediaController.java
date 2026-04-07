package com.example.chatApp.controller;

import com.example.chatApp.model.User;
import com.example.chatApp.dto.MediaUploadResponse;
import com.example.chatApp.repository.UserRepository;
import com.example.chatApp.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/media")
@Tag(name = "Media & Notifications", description = "File upload, download, and media management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class MediaController {

    private final MediaService mediaService;
    private final UserRepository userRepository;

    public MediaController(MediaService mediaService, UserRepository userRepository) {
        this.mediaService = mediaService;
        this.userRepository = userRepository;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload a file",
            description = "Upload a file or image. Supports all common file types up to 10MB."
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
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User uploader = resolveUser(userDetails);
            MediaUploadResponse response = mediaService.uploadFile(file, uploader);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to upload file: " + e.getMessage());
        }
    }

    @PostMapping(value = "/upload/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload multiple files",
            description = "Upload multiple files at once. Supports all common file types up to 10MB each."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Files uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid files"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<?> uploadMultipleFiles(
            @Parameter(description = "Files to upload", required = true)
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User uploader = resolveUser(userDetails);
            List<MediaUploadResponse> responses = new ArrayList<>();
            for (MultipartFile file : files) {
                responses.add(mediaService.uploadFile(file, uploader));
            }
            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to upload files: " + e.getMessage());
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
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = resolveUser(userDetails).getId();
            mediaService.deleteMedia(mediaId, userId);
            return ResponseEntity.ok().body("File deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private User resolveUser(UserDetails userDetails) {
        String email = userDetails.getUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found: " + email));
    }
}
