package com.example.chatApp.service;

import com.example.chatApp.model.UserStatus;
import com.example.chatApp.repository.UserStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserStatusService {

    private final UserStatusRepository userStatusRepository;

    public UserStatusService(UserStatusRepository userStatusRepository) {
        this.userStatusRepository = userStatusRepository;
    }

    @Transactional
    public void setOnline(Long userId) {
        UserStatus status = userStatusRepository.findById(userId)
                .orElse(UserStatus.builder().userId(userId).build());
        status.setIsOnline(true);
        status.setLastSeen(LocalDateTime.now());
        userStatusRepository.save(status);
    }

    @Transactional
    public void setOffline(Long userId) {
        UserStatus status = userStatusRepository.findById(userId)
                .orElse(UserStatus.builder().userId(userId).build());
        status.setIsOnline(false);
        status.setLastSeen(LocalDateTime.now());
        userStatusRepository.save(status);
    }

    public boolean isOnline(Long userId) {
        return userStatusRepository.findById(userId)
                .map(UserStatus::getIsOnline)
                .orElse(false);
    }

    public UserStatus getStatus(Long userId) {
        return userStatusRepository.findById(userId)
                .orElse(UserStatus.builder()
                        .userId(userId)
                        .isOnline(false)
                        .lastSeen(null)
                        .build());
    }
}
