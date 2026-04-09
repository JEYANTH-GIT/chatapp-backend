package com.example.chatApp.controller;
package com.example.chatApp.controller;

import com.example.chatApp.dto.NotificationPayload;
import com.example.chatApp.dto.ReactionPayload;
import com.example.chatApp.model.User;
import com.example.chatApp.repository.UserRepository;
import com.example.chatApp.service.NotificationService;
import com.example.chatApp.service.ReactionService;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Media & Notifications", description = "Emoji reactions, notification management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;
    private final ReactionService reactionService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService,
                                  ReactionService reactionService,
                                  UserRepository userRepository) {
        this.notificationService = notificationService;
        this.reactionService = reactionService;
        this.userRepository = userRepository;
    }

    // ========================
    // EMOJI REACTIONS
    // ========================

    @PostMapping("/api/media/messages/{messageId}/reactions")
    @Operation(
            summary = "Add emoji reaction",
            description = "Add an emoji reaction to a message."
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
            @Valid @RequestBody ReactionPayload payload,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = resolveUserId(userDetails);
            payload.setMessageId(messageId);
            payload.setUserId(userId);
            ReactionPayload response = reactionService.addReaction(
                    messageId, userId, payload.getEmoji());
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
    public ResponseEntity<?> removeReaction(
            @Parameter(description = "Message ID", required = true)
            @PathVariable Long messageId,
            @Parameter(description = "Reaction ID to remove", required = true)
            @PathVariable Long reactionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = resolveUserId(userDetails);
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
    public ResponseEntity<Page<NotificationPayload>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        Long userId = resolveUserId(userDetails);
        Page<NotificationPayload> notifications = notificationService.getNotifications(userId, page, size);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/api/notifications/unread-count")
    @Operation(
            summary = "Get unread notification count",
            description = "Get the count of unread notifications for the authenticated user."
    )
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUserId(userDetails);
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PutMapping("/api/notifications/{id}/read")
    @Operation(
            summary = "Mark notification as read",
            description = "Mark a single notification as read."
    )
    public ResponseEntity<?> markAsRead(
            @Parameter(description = "Notification ID", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = resolveUserId(userDetails);
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
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUserId(userDetails);
        int updated = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("markedAsRead", updated));
    }

    @DeleteMapping("/api/notifications/{id}")
    @Operation(
            summary = "Delete a notification",
            description = "Delete a notification by ID."
    )
    public ResponseEntity<?> deleteNotification(
            @Parameter(description = "Notification ID", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = resolveUserId(userDetails);
            notificationService.deleteNotification(id, userId);
            return ResponseEntity.ok(Map.of("message", "Notification deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Long resolveUserId(UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found: " + email));
        return user.getId();
    }
}
