package com.example.chatApp.media.service;

import com.example.chatApp.auth.model.User;
import com.example.chatApp.chat.model.Message;
import com.example.chatApp.media.dto.ReactionPayload;
import com.example.chatApp.media.model.MessageReaction;
import com.example.chatApp.media.repository.MessageReactionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReactionService {

    private final MessageReactionRepository messageReactionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public ReactionService(MessageReactionRepository messageReactionRepository) {
        this.messageReactionRepository = messageReactionRepository;
    }

    @Transactional
    public ReactionPayload addReaction(Long messageId, Long userId, String emoji) {
        // Check if user already reacted with same emoji
        messageReactionRepository.findByMessageIdAndUserId(messageId, userId)
                .ifPresent(existing -> {
                    messageReactionRepository.delete(existing);
                });

        Message message = entityManager.getReference(Message.class, messageId);
        User user = entityManager.getReference(User.class, userId);

        MessageReaction reaction = MessageReaction.builder()
                .message(message)
                .user(user)
                .emoji(emoji)
                .build();

        MessageReaction saved = messageReactionRepository.save(reaction);
        return mapToPayload(saved, user);
    }

    public List<ReactionPayload> getReactionsByMessageId(Long messageId) {
        return messageReactionRepository.findByMessageId(messageId)
                .stream()
                .map(r -> mapToPayload(r, r.getUser()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeReaction(Long reactionId, Long userId) {
        MessageReaction reaction = messageReactionRepository.findById(reactionId)
                .orElseThrow(() -> new RuntimeException("Reaction not found with id: " + reactionId));

        if (!reaction.getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to remove this reaction");
        }

        messageReactionRepository.delete(reaction);
    }

    private ReactionPayload mapToPayload(MessageReaction reaction, User user) {
        return ReactionPayload.builder()
                .id(reaction.getId())
                .messageId(reaction.getMessage().getId())
                .userId(user.getId())
                .username(user.getUsername())
                .profilePicture(user.getProfilePicture())
                .emoji(reaction.getEmoji())
                .createdAt(reaction.getCreatedAt())
                .build();
    }
}
