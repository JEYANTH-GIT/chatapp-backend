package com.example.chatApp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberRequest {

    @NotNull(message = "User ID is required")
    private Long userId;
}
