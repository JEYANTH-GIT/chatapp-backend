package com.example.chatApp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupRequest {

    @NotBlank(message = "Group name is required")
    @Size(min = 1, max = 100, message = "Group name must be between 1 and 100 characters")
    private String name;

    @Size(max = 300, message = "Description must not exceed 300 characters")
    private String description;

    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    private String avatar;
}
