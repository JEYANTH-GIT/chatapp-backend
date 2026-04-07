package com.example.chatApp.media.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionPayload {

    private Long id;

    @NotNull(message = "Message ID is required")
    private Long messageId;

    private Long userId;

    private String username;

    private String profilePicture;

    @NotBlank(message = "Emoji is required")
    @Size(max = 10, message = "Emoji must be at most 10 characters")
    private String emoji;

    private LocalDateTime createdAt;
}
