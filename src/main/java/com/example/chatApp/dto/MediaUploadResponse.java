package com.example.chatApp.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadResponse {

    private Long id;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String url;

    private Long uploaderId;

    private String uploaderUsername;

    private LocalDateTime uploadedAt;
}
