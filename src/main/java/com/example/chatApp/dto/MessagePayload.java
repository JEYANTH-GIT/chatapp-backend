package com.example.chatApp.dto;

import com.example.chatApp.model.Message;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for sending a message via WebSocket (/app/chat.send).
 * Owner: Mahalakshmi (Module 2)
 */
@Data
public class MessagePayload {

    @NotNull(message = "Chat ID or Group ID is required")
    private Long chatId;

    private Long groupId;

    @NotNull(message = "Sender ID is required")
    private Long senderId;

    private String content;

    private Long mediaId;

    private Message.MessageType messageType = Message.MessageType.TEXT;
}
