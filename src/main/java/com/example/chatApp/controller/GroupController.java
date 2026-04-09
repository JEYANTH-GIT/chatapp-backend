package com.example.chatApp.controller;

import com.example.chatApp.dto.GroupRequest;
import com.example.chatApp.dto.GroupResponse;
import com.example.chatApp.dto.MemberRequest;
import com.example.chatApp.dto.RoleUpdateRequest;
import com.example.chatApp.model.GroupMember;
import com.example.chatApp.model.User;
import com.example.chatApp.repository.UserRepository;
import com.example.chatApp.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
@Tag(name = "Groups & Users", description = "Group management and user operations")
@SecurityRequirement(name = "bearerAuth")
public class GroupController {

    private final GroupService groupService;
    private final UserRepository userRepository;
    private final com.example.chatApp.service.MessageService messageService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public GroupController(GroupService groupService, UserRepository userRepository,
                           com.example.chatApp.service.MessageService messageService,
                           org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate) {
        this.groupService = groupService;
        this.userRepository = userRepository;
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping
    @Operation(summary = "Create a new group", description = "Creates a new group and adds the creator as an ADMIN member")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Group created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<GroupResponse> createGroup(
            @Valid @RequestBody GroupRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUserId(userDetails);
        GroupResponse response = groupService.createGroup(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "Get group details", description = "Returns group information including member count")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Group details retrieved"),
            @ApiResponse(responseCode = "404", description = "Group not found")
    })
    public ResponseEntity<GroupResponse> getGroup(
            @PathVariable @Parameter(description = "Group ID") Long groupId) {
        GroupResponse response = groupService.getGroup(groupId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{groupId}")
    @Operation(summary = "Update group info", description = "Updates group name, description, or avatar. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Group updated successfully"),
            @ApiResponse(responseCode = "403", description = "Not an admin of the group"),
            @ApiResponse(responseCode = "404", description = "Group not found")
    })
    public ResponseEntity<GroupResponse> updateGroup(
            @PathVariable @Parameter(description = "Group ID") Long groupId,
            @Valid @RequestBody GroupRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUserId(userDetails);
        GroupResponse response = groupService.updateGroup(groupId, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{groupId}")
    @Operation(summary = "Delete a group", description = "Deletes the group and all its members. Only the group creator can delete.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Group deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Only the creator can delete the group"),
            @ApiResponse(responseCode = "404", description = "Group not found")
    })
    public ResponseEntity<Map<String, String>> deleteGroup(
            @PathVariable @Parameter(description = "Group ID") Long groupId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUserId(userDetails);
        groupService.deleteGroup(groupId, userId);
        return ResponseEntity.ok(Map.of("message", "Group deleted successfully"));
    }

    @PostMapping("/{groupId}/members")
    @Operation(summary = "Add a member to the group", description = "Adds a user as a MEMBER. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Member added successfully"),
            @ApiResponse(responseCode = "400", description = "User is already a member"),
            @ApiResponse(responseCode = "403", description = "Not an admin of the group"),
            @ApiResponse(responseCode = "404", description = "Group not found")
    })
    public ResponseEntity<Map<String, String>> addMember(
            @PathVariable @Parameter(description = "Group ID") Long groupId,
            @Valid @RequestBody MemberRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUserId(userDetails);
        groupService.addMember(groupId, request.getUserId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Member added successfully"));
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    @Operation(summary = "Remove a member from the group", description = "Removes the specified user from the group. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Member removed successfully"),
            @ApiResponse(responseCode = "403", description = "Not an admin of the group"),
            @ApiResponse(responseCode = "404", description = "Group or member not found")
    })
    public ResponseEntity<Map<String, String>> removeMember(
            @PathVariable @Parameter(description = "Group ID") Long groupId,
            @PathVariable @Parameter(description = "User ID to remove") Long memberId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUserId(userDetails);
        groupService.removeMember(groupId, memberId, userId);
        return ResponseEntity.ok(Map.of("message", "Member removed successfully"));
    }

    @PutMapping("/{groupId}/members/{memberId}/role")
    @Operation(summary = "Change member role", description = "Changes a member's role to ADMIN or MEMBER. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role updated successfully"),
            @ApiResponse(responseCode = "403", description = "Not an admin of the group"),
            @ApiResponse(responseCode = "404", description = "Group or member not found")
    })
    public ResponseEntity<Map<String, String>> updateMemberRole(
            @PathVariable @Parameter(description = "Group ID") Long groupId,
            @PathVariable @Parameter(description = "User ID to update") Long memberId,
            @Valid @RequestBody RoleUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUserId(userDetails);
        groupService.updateMemberRole(groupId, memberId, request.getRole(), userId);
        return ResponseEntity.ok(Map.of("message", "Role updated successfully"));
    }

    @GetMapping("/{groupId}/members")
    @Operation(summary = "Get group members", description = "Returns all members of the specified group")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Members retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Group not found")
    })
    public ResponseEntity<List<GroupMember>> getGroupMembers(
            @PathVariable @Parameter(description = "Group ID") Long groupId) {
        List<GroupMember> members = groupService.getGroupMembers(groupId);
        return ResponseEntity.ok(members);
    }

    // ── Group Messages ────────────────────────────────────────────────────────

    @GetMapping("/{groupId}/messages")
    @Operation(summary = "Get group message history", description = "Returns paginated messages for the group")
    public ResponseEntity<List<com.example.chatApp.dto.MessageResponse>> getGroupMessages(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        List<com.example.chatApp.dto.MessageResponse> messages = messageService.getGroupMessageHistory(groupId, page, size);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/{groupId}/messages")
    @Operation(summary = "Send a message to a group", description = "Sends a message to the group and broadcasts via WebSocket")
    public ResponseEntity<com.example.chatApp.dto.MessageResponse> sendGroupMessage(
            @PathVariable Long groupId,
            @RequestBody com.example.chatApp.dto.MessagePayload payload,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUserId(userDetails);
        payload.setGroupId(groupId);
        payload.setSenderId(userId);
        com.example.chatApp.dto.MessageResponse response = messageService.saveGroupMessage(payload);
        // Broadcast via WebSocket
        messagingTemplate.convertAndSend("/topic/group." + groupId, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Long resolveUserId(UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found: " + email));
        return user.getId();
    }
}
