package com.example.chatApp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for creating a new 1-to-1 chat (POST /api/chats).
 * Owner: Mahalakshmi (Module 2)
 */
@Data
public class CreateChatRequest {

    @NotNull(message = "Target user ID is required")
    private Long targetUserId;
}
