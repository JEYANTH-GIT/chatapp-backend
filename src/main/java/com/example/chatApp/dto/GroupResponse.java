package com.example.chatApp.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponse {

    private Long id;
    private String name;
    private String description;
    private String avatar;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long memberCount;
}
