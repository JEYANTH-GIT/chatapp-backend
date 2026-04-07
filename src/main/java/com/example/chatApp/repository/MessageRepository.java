package com.example.chatApp.repository;

import com.example.chatApp.model.Chat;
import com.example.chatApp.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Message entity.
 * Owner: Mahalakshmi (Module 2)
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Find paginated messages for a given chat, excluding soft-deleted messages.
     */
    Page<Message> findByChatAndIsDeletedFalseOrderBySentAtAsc(Chat chat, Pageable pageable);

    /**
     * Find all messages for a group (by groupId), excluding soft-deleted.
     */
    Page<Message> findByGroupIdAndIsDeletedFalseOrderBySentAtAsc(Long groupId, Pageable pageable);

    /**
     * Find the latest non-deleted message for a chat (used in chat list preview).
     */
    @Query("SELECT m FROM Message m WHERE m.chat = :chat AND m.isDeleted = false " +
           "ORDER BY m.sentAt DESC")
    List<Message> findLatestMessageByChat(@Param("chat") Chat chat, Pageable pageable);

    /**
     * Bulk-update status for all messages in a chat sent by the other user.
     */
    @Modifying
    @Query("UPDATE Message m SET m.status = :status " +
           "WHERE m.chat.id = :chatId AND m.sender.id != :userId AND m.status != :status")
    int updateMessageStatusByChatId(@Param("chatId") Long chatId,
                                     @Param("userId") Long userId,
                                     @Param("status") Message.MessageStatus status);
}
