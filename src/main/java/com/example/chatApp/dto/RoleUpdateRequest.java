package com.example.chatApp.dto;

import com.example.chatApp.model.GroupMember;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleUpdateRequest {

    @NotNull(message = "Role is required")
    private GroupMember.Role role;
}
