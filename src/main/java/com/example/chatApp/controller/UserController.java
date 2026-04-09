package com.example.chatApp.controller;

import com.example.chatApp.dto.ChatListItem;
import com.example.chatApp.dto.GroupResponse;
import com.example.chatApp.dto.UserSearchResult;
import com.example.chatApp.model.User;
import com.example.chatApp.model.UserStatus;
import com.example.chatApp.repository.UserRepository;
import com.example.chatApp.service.GroupService;
import com.example.chatApp.service.SearchService;
import com.example.chatApp.service.UserStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Groups & Users", description = "Group management and user operations")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final SearchService searchService;
    private final UserStatusService userStatusService;
    private final GroupService groupService;
    private final UserRepository userRepository;

    public UserController(SearchService searchService, UserStatusService userStatusService,
                          GroupService groupService, UserRepository userRepository) {
        this.searchService = searchService;
        this.userStatusService = userStatusService;
        this.groupService = groupService;
        this.userRepository = userRepository;
    }

    @GetMapping("/search")
    @Operation(summary = "Search users", description = "Search users by username or email. Returns up to 20 results.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results returned")
    })
    public ResponseEntity<List<UserSearchResult>> searchUsers(
            @RequestParam @Parameter(description = "Search query (username or email)") String q) {
        List<UserSearchResult> results = searchService.searchUsers(q);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{userId}/status")
    @Operation(summary = "Get user online status", description = "Returns whether a user is currently online and their last seen time")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status retrieved")
    })
    public ResponseEntity<UserStatus> getUserStatus(
            @PathVariable @Parameter(description = "User ID") Long userId) {
        UserStatus status = userStatusService.getStatus(userId);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/me/chats")
    @Operation(summary = "Get chat list for sidebar", description = "Returns the list of groups the current user belongs to, formatted for the sidebar chat list")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chat list retrieved")
    })
    public ResponseEntity<List<ChatListItem>> getChatList(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUserId(userDetails);

        List<GroupResponse> userGroups = groupService.getGroupsForUser(userId);

        List<ChatListItem> chatList = userGroups.stream()
                .map(group -> ChatListItem.builder()
                        .id(group.getId())
                        .name(group.getName())
                        .type("GROUP")
                        .avatarUrl(group.getAvatar())
                        .lastMessage(null)
                        .lastMessageTime(null)
                        .unreadCount(0L)
                        .isOnline(false)
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(chatList);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Long resolveUserId(UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found: " + email));
        return user.getId();
    }
}
