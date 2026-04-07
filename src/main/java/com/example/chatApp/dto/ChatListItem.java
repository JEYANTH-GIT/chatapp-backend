package com.example.chatApp.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatListItem {

    private Long id;
    private String name;
    private String type; // "CHAT" or "GROUP"
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Long unreadCount;
    private String avatarUrl;
    private Boolean isOnline;
}
