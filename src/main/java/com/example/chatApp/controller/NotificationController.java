package com.example.chatApp.controller;

import com.example.chatApp.dto.NotificationPayload;
import com.example.chatApp.dto.ReactionPayload;
import com.example.chatApp.service.NotificationService;
import com.example.chatApp.service.ReactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Media & Notifications", description = "Emoji reactions, notification management endpoints")
public class NotificationController {

    private final NotificationService notificationService;
    private final ReactionService reactionService;

    public NotificationController(NotificationService notificationService,
                                  ReactionService reactionService) {
        this.notificationService = notificationService;
        this.reactionService = reactionService;
    }

    // ========================
    // EMOJI REACTIONS
    // ========================

    @PostMapping("/api/media/messages/{messageId}/reactions")
    @Operation(
            summary = "Add emoji reaction",
            description = "Add an emoji reaction to a message. If user already reacted, replaces the previous reaction."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reaction added successfully",
                    content = @Content(schema = @Schema(implementation = ReactionPayload.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<?> addReaction(
            @Parameter(description = "Message ID to react to", required = true)
            @PathVariable Long messageId,
            @Valid @RequestBody ReactionPayload payload) {
        try {
            payload.setMessageId(messageId);
            ReactionPayload response = reactionService.addReaction(
                    messageId, payload.getUserId(), payload.getEmoji());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/api/media/messages/{messageId}/reactions")
    @Operation(
            summary = "Get reactions for a message",
            description = "Get all emoji reactions for a specific message."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reactions retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<ReactionPayload>> getReactions(
            @Parameter(description = "Message ID", required = true)
            @PathVariable Long messageId) {
        List<ReactionPayload> reactions = reactionService.getReactionsByMessageId(messageId);
        return ResponseEntity.ok(reactions);
    }

    @DeleteMapping("/api/media/messages/{messageId}/reactions/{reactionId}")
    @Operation(
            summary = "Remove emoji reaction",
            description = "Remove an emoji reaction. Only the user who added the reaction can remove it."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reaction removed successfully"),
            @ApiResponse(responseCode = "403", description = "Not authorized"),
            @ApiResponse(responseCode = "404", description = "Reaction not found")
    })
    public ResponseEntity<?> removeReaction(
            @Parameter(description = "Message ID", required = true)
            @PathVariable Long messageId,
            @Parameter(description = "Reaction ID to remove", required = true)
            @PathVariable Long reactionId,
            @Parameter(description = "User ID of requester", required = true)
            @RequestParam("userId") Long userId) {
        try {
            reactionService.removeReaction(reactionId, userId);
            return ResponseEntity.ok().body("Reaction removed successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ========================
    // NOTIFICATIONS
    // ========================

    @GetMapping("/api/notifications")
    @Operation(
            summary = "Get all notifications",
            description = "Get paginated notifications for the authenticated user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<NotificationPayload>> getNotifications(
            @Parameter(description = "Recipient user ID", required = true)
            @RequestParam("userId") Long userId,
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        Page<NotificationPayload> notifications = notificationService.getNotifications(userId, page, size);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/api/notifications/unread-count")
    @Operation(
            summary = "Get unread notification count",
            description = "Get the count of unread notifications for the authenticated user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Unread count retrieved")
    })
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @Parameter(description = "User ID", required = true)
            @RequestParam("userId") Long userId) {
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PutMapping("/api/notifications/{id}/read")
    @Operation(
            summary = "Mark notification as read",
            description = "Mark a single notification as read."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notification marked as read",
                    content = @Content(schema = @Schema(implementation = NotificationPayload.class))),
            @ApiResponse(responseCode = "404", description = "Notification not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<?> markAsRead(
            @Parameter(description = "Notification ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "User ID of requester", required = true)
            @RequestParam("userId") Long userId) {
        try {
            NotificationPayload payload = notificationService.markAsRead(id, userId);
            return ResponseEntity.ok(payload);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/api/notifications/read-all")
    @Operation(
            summary = "Mark all notifications as read",
            description = "Mark all unread notifications as read for the authenticated user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All notifications marked as read")
    })
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @Parameter(description = "User ID", required = true)
            @RequestParam("userId") Long userId) {
        int updated = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("markedAsRead", updated));
    }
}
