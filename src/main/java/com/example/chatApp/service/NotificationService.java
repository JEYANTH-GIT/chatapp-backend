package com.example.chatApp.service;
<<<<<<< HEAD

import com.example.chatApp.model.User;
import com.example.chatApp.dto.NotificationPayload;
import com.example.chatApp.model.Notification;
import com.example.chatApp.model.Notification.NotificationType;
import com.example.chatApp.repository.NotificationRepository;

=======

import com.example.chatApp.model.User;
import com.example.chatApp.dto.NotificationPayload;
import com.example.chatApp.model.Notification;
import com.example.chatApp.model.Notification.NotificationType;
import com.example.chatApp.repository.NotificationRepository;
>>>>>>> 336049e9327ef3bc762643b5dee206ef27479048
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    public NotificationService(NotificationRepository notificationRepository,
                               SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Creates a notification and pushes it via WebSocket to the recipient.
     * This is the integration point — Maha's MessageService calls this method.
     * Method signature contract: createNotification(Long recipientId, Long actorId, NotificationType type, Long referenceId)
     */
    @Transactional
    public NotificationPayload createNotification(Long recipientId, Long actorId,
                                                   NotificationType type, Long referenceId) {
        User recipient = entityManager.getReference(User.class, recipientId);
        User actor = actorId != null ? entityManager.getReference(User.class, actorId) : null;

        String content = generateContent(type, actor);

        Notification notification = Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(type)
                .referenceId(referenceId)
                .content(content)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        NotificationPayload payload = mapToPayload(saved);

        // Push real-time notification via WebSocket
        messagingTemplate.convertAndSendToUser(
                recipientId.toString(),
                "/queue/notifications",
                payload
        );

        return payload;
    }

    public Page<NotificationPayload> getNotifications(Long recipientId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable)
                .map(this::mapToPayload);
    }

    public long getUnreadCount(Long recipientId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(recipientId);
    }

    @Transactional
    public NotificationPayload markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + notificationId));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to modify this notification");
        }

        notification.setIsRead(true);
        Notification saved = notificationRepository.save(notification);
        return mapToPayload(saved);
    }

    @Transactional
    public int markAllAsRead(Long recipientId) {
        return notificationRepository.markAllAsRead(recipientId);
    }

    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + notificationId));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to delete this notification");
        }

        notificationRepository.delete(notification);
    }

    private String generateContent(NotificationType type, User actor) {
        String actorName = actor != null ? actor.getUsername() : "Someone";
        return switch (type) {
            case NEW_MESSAGE -> actorName + " sent you a message";
            case GROUP_INVITE -> actorName + " invited you to a group";
            case REACTION -> actorName + " reacted to your message";
            case MENTION -> actorName + " mentioned you";
            case SYSTEM -> "System notification";
        };
    }

    private NotificationPayload mapToPayload(Notification notification) {
        return NotificationPayload.builder()
                .id(notification.getId())
                .recipientId(notification.getRecipient().getId())
                .actorId(notification.getActor() != null ? notification.getActor().getId() : null)
                .actorUsername(notification.getActor() != null ? notification.getActor().getUsername() : null)
                .actorProfilePicture(notification.getActor() != null ? notification.getActor().getProfilePicture() : null)
                .type(notification.getType())
                .referenceId(notification.getReferenceId())
                .content(notification.getContent())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
