package com.example.chatApp.service;

import com.example.chatApp.model.User;
import com.example.chatApp.repository.UserRepository;
import com.example.chatApp.dto.MessagePayload;
import com.example.chatApp.dto.MessageResponse;
import com.example.chatApp.model.Chat;
import com.example.chatApp.model.Message;
import com.example.chatApp.repository.ChatRepository;
import com.example.chatApp.repository.MessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for message operations.
 * Owner: Mahalakshmi (Module 2)
 *
 * Integration Notes:
 * - After saving a message, calls NotificationService (Kavi, Module 4)
 *   Method: notificationService.createNotification(recipientId, actorId, NEW_MESSAGE, messageId)
 *   NOTE: NotificationService is owned by Kavi — inject via constructor when Kavi's module is ready.
 */
@Service
@Transactional
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;

    public MessageService(MessageRepository messageRepository,
                          ChatRepository chatRepository,
                          UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
    }

    /**
     * Saves a new message from a WebSocket payload.
     * Called by ChatWebSocketController when a message arrives at /app/chat.send
     */
    public MessageResponse saveMessage(MessagePayload payload) {
        Chat chat = chatRepository.findById(payload.getChatId())
            .orElseThrow(() -> new RuntimeException("Chat not found: " + payload.getChatId()));

        User sender = userRepository.findById(payload.getSenderId())
            .orElseThrow(() -> new RuntimeException("Sender not found: " + payload.getSenderId()));

        Message message = Message.builder()
            .chat(chat)
            .groupId(payload.getGroupId())
            .sender(sender)
            .content(payload.getContent())
            .mediaId(payload.getMediaId())
            .messageType(payload.getMessageType() != null ? payload.getMessageType() : Message.MessageType.TEXT)
            .status(Message.MessageStatus.SENT)
            .isDeleted(false)
            .build();

        Message saved = messageRepository.save(message);

        // ── Integration Point: Notify recipient ────────────────────────────
        // TODO (integrate with Kavi's Module 4 when ready):
        // User recipient = chat.getParticipantOne().getId().equals(sender.getId())
        //     ? chat.getParticipantTwo() : chat.getParticipantOne();
        // notificationService.createNotification(recipient.getId(), sender.getId(),
        //     NotificationType.NEW_MESSAGE, saved.getId());

        return mapToMessageResponse(saved);
    }

    /**
     * Get paginated message history for a chat.
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessageHistory(Long chatId, int page, int size) {
        Chat chat = chatRepository.findById(chatId)
            .orElseThrow(() -> new RuntimeException("Chat not found: " + chatId));

        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messages = messageRepository
            .findByChatAndIsDeletedFalseOrderBySentAtAsc(chat, pageable);

        return messages.getContent()
            .stream()
            .map(this::mapToMessageResponse)
            .collect(Collectors.toList());
    }

    /**
     * Update delivery/seen status for a specific message.
     */
    public MessageResponse updateMessageStatus(Long messageId, Message.MessageStatus newStatus) {
        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("Message not found: " + messageId));

        // Only allow forward status transitions: SENT → DELIVERED → SEEN
        if (newStatus == Message.MessageStatus.SEEN ||
            (newStatus == Message.MessageStatus.DELIVERED
                && message.getStatus() == Message.MessageStatus.SENT)) {
            message.setStatus(newStatus);
            message = messageRepository.save(message);
        }

        return mapToMessageResponse(message);
    }

    /**
     * Bulk update all unread messages in a chat as SEEN for a given user.
     * Called when a user opens a chat window.
     */
    public int markAllAsSeen(Long chatId, Long userId) {
        return messageRepository.updateMessageStatusByChatId(
            chatId, userId, Message.MessageStatus.SEEN);
    }

    /**
     * Soft-delete a message by ID.
     */
    public void softDeleteMessage(Long messageId, Long requestingUserId) {
        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("Message not found: " + messageId));

        if (!message.getSender().getId().equals(requestingUserId)) {
            throw new SecurityException("You can only delete your own messages");
        }

        message.setIsDeleted(true);
        messageRepository.save(message);
    }

    // ── Mapper ───────────────────────────────────────────────────────────────

    public MessageResponse mapToMessageResponse(Message message) {
        return MessageResponse.builder()
            .id(message.getId())
            .chatId(message.getChat() != null ? message.getChat().getId() : null)
            .groupId(message.getGroupId())
            .senderId(message.getSender().getId())
            .senderUsername(message.getSender().getUsername())
            .senderProfilePicture(message.getSender().getProfilePicture())
            .content(message.getContent())
            .mediaId(message.getMediaId())
            .messageType(message.getMessageType())
            .status(message.getStatus())
            .isDeleted(message.getIsDeleted())
            .sentAt(message.getSentAt())
            .build();
    }
}
