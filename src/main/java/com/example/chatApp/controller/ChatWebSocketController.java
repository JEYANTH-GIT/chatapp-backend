package com.example.chatApp.controller;

import com.example.chatApp.dto.MessagePayload;
import com.example.chatApp.dto.MessageResponse;
import com.example.chatApp.dto.MessageStatusPayload;
import com.example.chatApp.dto.TypingPayload;
import com.example.chatApp.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * WebSocket STOMP Controller for real-time messaging.
 * Owner: Mahalakshmi (Module 2)
 *
 * Topics owned:
 *   /app/chat.send        → /topic/chat.{chatId}
 *   /app/chat.typing      → /topic/typing.{chatId}
 *   /app/message.status   → /topic/chat.{chatId}
 */
@Controller
@Tag(name = "Chat & Messaging", description = "WebSocket STOMP endpoints for real-time chat")
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;

    public ChatWebSocketController(SimpMessagingTemplate messagingTemplate,
                                   MessageService messageService) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
    }

    /**
     * Endpoint: /app/chat.send
     * Direction: Client → Server → /topic/chat.{chatId}
     *
     * Client sends a MessagePayload. Server persists it and broadcasts
     * the saved MessageResponse to all subscribers of /topic/chat.{chatId}.
     */
    @Operation(summary = "Send a chat message via WebSocket",
               description = "Client-to-server. Persists message and broadcasts to /topic/chat.{chatId}")
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload MessagePayload payload,
                            SimpMessageHeaderAccessor headerAccessor) {
        MessageResponse savedMessage = messageService.saveMessage(payload);

        messagingTemplate.convertAndSend(
            "/topic/chat." + savedMessage.getChatId(),
            savedMessage
        );
    }

    /**
     * Endpoint: /app/chat.typing
     * Direction: Client → Server → /topic/typing.{chatId}
     *
     * Broadcasts typing indicator to the other participant.
     * NOT persisted to DB.
     */
    @Operation(summary = "Broadcast typing indicator via WebSocket",
               description = "Client-to-server. Broadcasts typing event to /topic/typing.{chatId}")
    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload TypingPayload payload) {
        messagingTemplate.convertAndSend(
            "/topic/typing." + payload.getChatId(),
            payload
        );
    }

    /**
     * Endpoint: /app/message.status
     * Direction: Client → Server → /topic/chat.{chatId}
     *
     * Updates individual message status (DELIVERED / SEEN) and
     * broadcasts the updated MessageResponse back to the chat topic.
     */
    @Operation(summary = "Update message status via WebSocket",
               description = "Client-to-server. Updates DELIVERED/SEEN status and broadcasts to /topic/chat.{chatId}")
    @MessageMapping("/message.status")
    public void updateMessageStatus(@Payload MessageStatusPayload payload) {
        MessageResponse updated = messageService.updateMessageStatus(
            payload.getMessageId(),
            payload.getStatus()
        );

        messagingTemplate.convertAndSend(
            "/topic/chat." + payload.getChatId(),
            updated
        );

        // Also trigger bulk seen when user opens the chat
        if (payload.getStatus() == com.example.chatApp.model.Message.MessageStatus.SEEN) {
            messageService.markAllAsSeen(payload.getChatId(), payload.getUserId());
        }
    }
}
