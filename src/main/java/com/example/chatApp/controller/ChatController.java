package com.example.chatApp.controller;

import com.example.chatApp.dto.ChatResponse;
import com.example.chatApp.dto.CreateChatRequest;
import com.example.chatApp.dto.MessageResponse;
import com.example.chatApp.model.Message;
import com.example.chatApp.service.ChatService;
import com.example.chatApp.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for 1-to-1 Chat operations.
 * Owner: Mahalakshmi (Module 2)
 * Base path: /api/chats
 * Swagger Tag: "Chat & Messaging"
 */
@RestController
@RequestMapping("/api/chats")
@Tag(name = "Chat & Messaging", description = "1-to-1 chat and message history APIs")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final ChatService chatService;
    private final MessageService messageService;

    public ChatController(ChatService chatService, MessageService messageService) {
        this.chatService = chatService;
        this.messageService = messageService;
    }

    // ── POST /api/chats ───────────────────────────────────────────────────────

    @Operation(
        summary = "Create or get a 1-to-1 chat",
        description = "Creates a new chat between the authenticated user and the target user, or returns the existing one."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Chat created or retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request body"),
        @ApiResponse(responseCode = "401", description = "Unauthorized — JWT missing or invalid"),
        @ApiResponse(responseCode = "404", description = "Target user not found")
    })
    @PostMapping
    public ResponseEntity<ChatResponse> createOrGetChat(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateChatRequest request) {
        Long currentUserId = extractUserId(userDetails);
        ChatResponse response = chatService.getOrCreateChat(currentUserId, request);
        return ResponseEntity.ok(response);
    }

    // ── GET /api/chats/{chatId} ───────────────────────────────────────────────

    @Operation(
        summary = "Get chat details by ID",
        description = "Returns metadata for a specific chat including participants."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Chat details retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Chat not found")
    })
    @GetMapping("/{chatId}")
    public ResponseEntity<ChatResponse> getChatById(
            @PathVariable Long chatId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long currentUserId = extractUserId(userDetails);
        ChatResponse response = chatService.getChatById(chatId, currentUserId);
        return ResponseEntity.ok(response);
    }

    // ── GET /api/chats/{chatId}/messages ─────────────────────────────────────

    @Operation(
        summary = "Get paginated message history for a chat",
        description = "Returns messages ordered oldest-to-newest. Use `page` and `size` for pagination."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Message list returned"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Chat not found")
    })
    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<MessageResponse>> getMessageHistory(
            @PathVariable Long chatId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Messages per page") @RequestParam(defaultValue = "30") int size) {
        List<MessageResponse> messages = messageService.getMessageHistory(chatId, page, size);
        return ResponseEntity.ok(messages);
    }

    // ── PUT /api/chats/messages/{messageId}/status ────────────────────────────

    @Operation(
        summary = "Update message status",
        description = "Mark a message as DELIVERED or SEEN. Also triggers bulk-seen for entire chat."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated"),
        @ApiResponse(responseCode = "400", description = "Invalid status value"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Message not found")
    })
    @PutMapping("/messages/{messageId}/status")
    public ResponseEntity<MessageResponse> updateMessageStatus(
            @PathVariable Long messageId,
            @Parameter(description = "New status: DELIVERED or SEEN")
            @RequestParam Message.MessageStatus status) {
        MessageResponse response = messageService.updateMessageStatus(messageId, status);
        return ResponseEntity.ok(response);
    }

    // ── DELETE /api/chats/messages/{messageId} ────────────────────────────────

    @Operation(
        summary = "Soft-delete a message",
        description = "Marks the message as deleted. Only the original sender can delete their message."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Message deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden — not the message sender"),
        @ApiResponse(responseCode = "404", description = "Message not found")
    })
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long currentUserId = extractUserId(userDetails);
        messageService.softDeleteMessage(messageId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Extracts the user ID from the UserDetails principal.
     * Works with Karthik's CustomUserDetailsService which sets username as the user ID string.
     */
    private Long extractUserId(UserDetails userDetails) {
        return Long.parseLong(userDetails.getUsername());
    }
}
