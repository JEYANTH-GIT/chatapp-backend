package com.example.chatApp.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResult {

    private Long id;
    private String username;
    private String email;
    private String profilePicture;
    private Boolean isOnline;
}
