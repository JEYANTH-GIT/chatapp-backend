package com.example.chatApp.repository;

import com.example.chatApp.auth.model.User;
import com.example.chatApp.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

/**
 * Repository for Chat entity.
 * Owner: Mahalakshmi (Module 2)
 */
@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    /**
     * Find an existing 1-to-1 chat between two users (regardless of order).
     */
    @Query("SELECT c FROM Chat c WHERE " +
           "(c.participantOne = :userA AND c.participantTwo = :userB) OR " +
           "(c.participantOne = :userB AND c.participantTwo = :userA)")
    Optional<Chat> findByParticipants(@Param("userA") User userA,
                                      @Param("userB") User userB);

    /**
     * Find all chats where the given user is a participant.
     */
    @Query("SELECT c FROM Chat c WHERE " +
           "c.participantOne = :user OR c.participantTwo = :user " +
           "ORDER BY c.createdAt DESC")
    List<Chat> findAllByUser(@Param("user") User user);
}
