package com.example.chatApp.model;

import com.example.chatApp.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a 1-to-1 chat session between two users.
 * Owner: Mahalakshmi (Module 2)
 * DB Table: chats
 */
@Entity
@Table(name = "chats",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"participant_one_id", "participant_two_id"}
    )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_one_id", nullable = false)
    private User participantOne;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_two_id", nullable = false)
    private User participantTwo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
