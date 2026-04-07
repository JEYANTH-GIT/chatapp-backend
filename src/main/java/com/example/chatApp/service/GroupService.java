package com.example.chatApp.service;

import com.example.chatApp.dto.GroupRequest;
import com.example.chatApp.dto.GroupResponse;
import com.example.chatApp.exception.ResourceNotFoundException;
import com.example.chatApp.exception.UnauthorizedException;
import com.example.chatApp.model.Group;
import com.example.chatApp.model.GroupMember;
import com.example.chatApp.repository.GroupMemberRepository;
import com.example.chatApp.repository.GroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    public GroupService(GroupRepository groupRepository, GroupMemberRepository groupMemberRepository) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    @Transactional
    public GroupResponse createGroup(GroupRequest request, Long creatorUserId) {
        Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .avatar(request.getAvatar())
                .createdBy(creatorUserId)
                .build();

        Group saved = groupRepository.save(group);

        // Auto-add creator as ADMIN
        GroupMember adminMember = GroupMember.builder()
                .groupId(saved.getId())
                .userId(creatorUserId)
                .role(GroupMember.Role.ADMIN)
                .build();
        groupMemberRepository.save(adminMember);

        return mapToResponse(saved, 1L);
    }

    public GroupResponse getGroup(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", groupId));
        long memberCount = groupMemberRepository.countByGroupId(groupId);
        return mapToResponse(group, memberCount);
    }

    @Transactional
    public GroupResponse updateGroup(Long groupId, GroupRequest request, Long requestingUserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", groupId));

        validateAdmin(groupId, requestingUserId);

        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setAvatar(request.getAvatar());

        Group updated = groupRepository.save(group);
        long memberCount = groupMemberRepository.countByGroupId(groupId);
        return mapToResponse(updated, memberCount);
    }

    @Transactional
    public void deleteGroup(Long groupId, Long requestingUserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", groupId));

        if (!group.getCreatedBy().equals(requestingUserId)) {
            throw new UnauthorizedException("Only the group creator can delete the group");
        }

        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        groupMemberRepository.deleteAll(members);
        groupRepository.delete(group);
    }

    @Transactional
    public void addMember(Long groupId, Long userId, Long requestingUserId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", groupId));

        validateAdmin(groupId, requestingUserId);

        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new IllegalArgumentException("User is already a member of this group");
        }

        GroupMember member = GroupMember.builder()
                .groupId(groupId)
                .userId(userId)
                .role(GroupMember.Role.MEMBER)
                .build();
        groupMemberRepository.save(member);
    }

    @Transactional
    public void removeMember(Long groupId, Long userId, Long requestingUserId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", groupId));

        validateAdmin(groupId, requestingUserId);

        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in this group"));

        groupMemberRepository.delete(member);
    }

    @Transactional
    public void updateMemberRole(Long groupId, Long userId, GroupMember.Role newRole, Long requestingUserId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", groupId));

        validateAdmin(groupId, requestingUserId);

        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in this group"));

        member.setRole(newRole);
        groupMemberRepository.save(member);
    }

    public List<GroupMember> getGroupMembers(Long groupId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", groupId));
        return groupMemberRepository.findByGroupId(groupId);
    }

    public List<GroupResponse> getGroupsForUser(Long userId) {
        List<GroupMember> memberships = groupMemberRepository.findByUserId(userId);
        return memberships.stream()
                .map(membership -> {
                    Group group = groupRepository.findById(membership.getGroupId())
                            .orElse(null);
                    if (group == null) return null;
                    long memberCount = groupMemberRepository.countByGroupId(group.getId());
                    return mapToResponse(group, memberCount);
                })
                .filter(g -> g != null)
                .collect(Collectors.toList());
    }

    private void validateAdmin(Long groupId, Long userId) {
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new UnauthorizedException("You are not a member of this group"));
        if (member.getRole() != GroupMember.Role.ADMIN) {
            throw new UnauthorizedException("Only group admins can perform this action");
        }
    }

    private GroupResponse mapToResponse(Group group, Long memberCount) {
        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .avatar(group.getAvatar())
                .createdBy(group.getCreatedBy())
                .createdAt(group.getCreatedAt())
                .memberCount(memberCount)
                .build();
    }
}
