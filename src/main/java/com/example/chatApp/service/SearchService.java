package com.example.chatApp.service;

import com.example.chatApp.dto.UserSearchResult;
import com.example.chatApp.repository.UserStatusRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @PersistenceContext
    private EntityManager entityManager;

    private final UserStatusRepository userStatusRepository;

    public SearchService(UserStatusRepository userStatusRepository) {
        this.userStatusRepository = userStatusRepository;
    }

    @SuppressWarnings("unchecked")
    public List<UserSearchResult> searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        String sql = "SELECT id, username, email, profile_picture FROM users " +
                "WHERE username LIKE :query OR email LIKE :query " +
                "ORDER BY username ASC LIMIT 20";

        Query nativeQuery = entityManager.createNativeQuery(sql);
        nativeQuery.setParameter("query", "%" + query.trim() + "%");

        List<Object[]> results = nativeQuery.getResultList();

        return results.stream()
                .map(row -> {
                    Long userId = ((Number) row[0]).longValue();
                    boolean online = userStatusRepository.findById(userId)
                            .map(status -> Boolean.TRUE.equals(status.getIsOnline()))
                            .orElse(false);

                    return UserSearchResult.builder()
                            .id(userId)
                            .username((String) row[1])
                            .email((String) row[2])
                            .profilePicture((String) row[3])
                            .isOnline(online)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
