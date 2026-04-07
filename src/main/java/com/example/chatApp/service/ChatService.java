package com.example.chatApp.service;

import com.example.chatApp.auth.model.User;
import com.example.chatApp.auth.repository.UserRepository;
import com.example.chatApp.dto.ChatResponse;
import com.example.chatApp.dto.CreateChatRequest;
import com.example.chatApp.dto.MessageResponse;
import com.example.chatApp.model.Chat;
import com.example.chatApp.model.Message;
import com.example.chatApp.repository.ChatRepository;
import com.example.chatApp.repository.MessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for 1-to-1 chat management.
 * Owner: Mahalakshmi (Module 2)
 *
 * Integration Notes:
 * - UserRepository owned by Karthik (Module 1)
 * - Calls NotificationService (Kavi, Module 4) via MessageService
 */
@Service
@Transactional
public class ChatService {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    public ChatService(ChatRepository chatRepository,
                       UserRepository userRepository,
                       MessageRepository messageRepository) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
    }

    /**
     * Creates a new 1-to-1 chat or returns the existing one.
     */
    public ChatResponse getOrCreateChat(Long currentUserId, CreateChatRequest request) {
        User currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new RuntimeException("Current user not found with id: " + currentUserId));
        User targetUser = userRepository.findById(request.getTargetUserId())
            .orElseThrow(() -> new RuntimeException("Target user not found with id: " + request.getTargetUserId()));

        Optional<Chat> existing = chatRepository.findByParticipants(currentUser, targetUser);
        Chat chat = existing.orElseGet(() -> {
            Chat newChat = Chat.builder()
                .participantOne(currentUser)
                .participantTwo(targetUser)
                .build();
            return chatRepository.save(newChat);
        });

        return mapToChatResponse(chat, currentUserId);
    }

    /**
     * Get chat details by chatId.
     */
    @Transactional(readOnly = true)
    public ChatResponse getChatById(Long chatId, Long currentUserId) {
        Chat chat = chatRepository.findById(chatId)
            .orElseThrow(() -> new RuntimeException("Chat not found with id: " + chatId));
        return mapToChatResponse(chat, currentUserId);
    }

    /**
     * Get all chats for the currently authenticated user.
     */
    @Transactional(readOnly = true)
    public List<ChatResponse> getAllChatsForUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        return chatRepository.findAllByUser(user)
            .stream()
            .map(chat -> mapToChatResponse(chat, userId))
            .collect(Collectors.toList());
    }

    // ── Mapper ───────────────────────────────────────────────────────────────

    public ChatResponse mapToChatResponse(Chat chat, Long currentUserId) {
        // Get the "other" participant's last message preview
        List<Message> latestMessages = messageRepository
            .findLatestMessageByChat(chat, PageRequest.of(0, 1));

        String lastMessage = null;
        java.time.LocalDateTime lastMessageAt = null;
        if (!latestMessages.isEmpty()) {
            Message lm = latestMessages.get(0);
            lastMessage = lm.getContent() != null ? lm.getContent() : "[" + lm.getMessageType().name() + "]";
            lastMessageAt = lm.getSentAt();
        }

        return ChatResponse.builder()
            .id(chat.getId())
            .participantOneId(chat.getParticipantOne().getId())
            .participantOneUsername(chat.getParticipantOne().getUsername())
            .participantOneProfilePicture(chat.getParticipantOne().getProfilePicture())
            .participantTwoId(chat.getParticipantTwo().getId())
            .participantTwoUsername(chat.getParticipantTwo().getUsername())
            .participantTwoProfilePicture(chat.getParticipantTwo().getProfilePicture())
            .lastMessage(lastMessage)
            .lastMessageAt(lastMessageAt)
            .otherUserOnline(false) // populated by Jeyanth's UserStatusService at Module 3 integration
            .createdAt(chat.getCreatedAt())
            .build();
    }
}
