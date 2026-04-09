package com.example.chatApp.model;

<<<<<<< HEAD
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * User entity — owned by Karthik (Module 1: Authentication).
 * This is a stub for compilation. Karthik will complete this entity.
 */
=======
import com.example.chatApp.enums.AuthProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

>>>>>>> 336049e9327ef3bc762643b5dee206ef27479048
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

<<<<<<< HEAD
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "password_hash")
    private String passwordHash;
=======
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(length = 20)
    private String phone;

    @Column(name = "profile_picture", length = 500)
    private String profilePicture;
>>>>>>> 336049e9327ef3bc762643b5dee206ef27479048

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider")
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

<<<<<<< HEAD
    @Column(name = "profile_picture", length = 500)
    private String profilePicture;

    @Column(length = 300)
    private String bio;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum AuthProvider {
        LOCAL, GOOGLE, PHONE
    }
=======
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
>>>>>>> 336049e9327ef3bc762643b5dee206ef27479048
}
