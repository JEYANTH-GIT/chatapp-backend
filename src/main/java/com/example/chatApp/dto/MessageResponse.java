package com.example.chatApp.dto;

import com.example.chatApp.model.Message;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for Message entity.
 * Owner: Mahalakshmi (Module 2)
 */
@Data
@Builder
public class MessageResponse {

    private Long id;
    private Long chatId;
    private Long groupId;
    private Long senderId;
    private String senderUsername;
    private String senderProfilePicture;
    private String content;
    private Long mediaId;
    private Message.MessageType messageType;
    private Message.MessageStatus status;
    private boolean isDeleted;
    private LocalDateTime sentAt;
}
