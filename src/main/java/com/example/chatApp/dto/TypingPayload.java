package com.example.chatApp.dto;

import lombok.Data;

/**
 * DTO for broadcasting typing events via WebSocket (/app/chat.typing).
 * Owner: Mahalakshmi (Module 2)
 */
@Data
public class TypingPayload {

    private Long chatId;
    private Long senderId;
    private String senderUsername;
    private boolean typing;
}
