package com.example.chatApp.model;

import com.example.chatApp.auth.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a single message in a 1-to-1 or group chat.
 * Owner: Mahalakshmi (Module 2)
 * DB Table: messages
 *
 * Integration notes:
 *  - mediaId references media_files.id (owned by Kavi, Module 4)
 *  - groupId references groups.id (owned by Jeyanth, Module 3) — nullable
 */
@Entity
@Table(name = "messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id")
    private Chat chat;

    /**
     * Nullable — populated when the message belongs to a group chat.
     * References groups.id (Module 3 — Jeyanth). Stored as plain Long
     * to avoid cross-module JPA relationship.
     */
    @Column(name = "group_id")
    private Long groupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * Nullable FK to media_files.id (Module 4 — Kavi).
     * Stored as plain Long to avoid cross-module JPA dependency.
     */
    @Column(name = "media_id")
    private Long mediaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "sent_at", updatable = false)
    private LocalDateTime sentAt;

    // ── Enums ────────────────────────────────────────────────────────────────

    public enum MessageType {
        TEXT, IMAGE, FILE, AUDIO, VIDEO
    }

    public enum MessageStatus {
        SENT, DELIVERED, SEEN
    }
}
