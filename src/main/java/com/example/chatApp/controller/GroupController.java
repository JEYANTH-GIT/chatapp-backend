package com.example.chatApp.controller;

import com.example.chatApp.dto.GroupRequest;
import com.example.chatApp.dto.GroupResponse;
import com.example.chatApp.dto.MemberRequest;
import com.example.chatApp.dto.RoleUpdateRequest;
import com.example.chatApp.model.GroupMember;
import com.example.chatApp.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
@Tag(name = "Groups & Users", description = "Group management and user operations")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
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
            @RequestHeader(value = "X-User-Id", defaultValue = "1") @Parameter(description = "Authenticated user ID") Long userId) {
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
            @RequestHeader(value = "X-User-Id", defaultValue = "1") @Parameter(description = "Authenticated user ID") Long userId) {
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
            @RequestHeader(value = "X-User-Id", defaultValue = "1") @Parameter(description = "Authenticated user ID") Long userId) {
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
            @RequestHeader(value = "X-User-Id", defaultValue = "1") @Parameter(description = "Authenticated user ID") Long userId) {
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
            @RequestHeader(value = "X-User-Id", defaultValue = "1") @Parameter(description = "Authenticated user ID") Long userId) {
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
            @RequestHeader(value = "X-User-Id", defaultValue = "1") @Parameter(description = "Authenticated user ID") Long userId) {
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
}
