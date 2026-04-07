package com.example.chatApp.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for Chat entity.
 * Owner: Mahalakshmi (Module 2)
 */
@Data
@Builder
public class ChatResponse {

    private Long id;

    // Participant One
    private Long participantOneId;
    private String participantOneUsername;
    private String participantOneProfilePicture;

    // Participant Two
    private Long participantTwoId;
    private String participantTwoUsername;
    private String participantTwoProfilePicture;

    // Last message preview
    private String lastMessage;
    private LocalDateTime lastMessageAt;

    // Online status — populated by Jeyanth's UserStatusService (Module 3)
    private boolean otherUserOnline;

    private LocalDateTime createdAt;
}
