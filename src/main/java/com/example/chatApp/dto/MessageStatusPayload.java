package com.example.chatApp.dto;

import com.example.chatApp.model.Message;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for updating message DELIVERED / SEEN status via WebSocket (/app/message.status).
 * Owner: Mahalakshmi (Module 2)
 */
@Data
public class MessageStatusPayload {

    @NotNull(message = "Message ID is required")
    private Long messageId;

    @NotNull(message = "Chat ID is required")
    private Long chatId;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Status is required")
    private Message.MessageStatus status;
}
