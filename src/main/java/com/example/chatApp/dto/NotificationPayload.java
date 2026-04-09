package com.example.chatApp.dto;

<<<<<<< HEAD
=======
import com.example.chatApp.model.Notification.NotificationType;
>>>>>>> 336049e9327ef3bc762643b5dee206ef27479048
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.example.chatApp.model.Notification.NotificationType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPayload {

    private Long id;

    private Long recipientId;

    private Long actorId;

    private String actorUsername;

    private String actorProfilePicture;

    private NotificationType type;

    private Long referenceId;

    private String content;

    private Boolean isRead;

    private LocalDateTime createdAt;
}
