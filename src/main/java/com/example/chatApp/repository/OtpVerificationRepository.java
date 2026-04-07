package com.example.chatApp.repository;

import com.example.chatApp.model.OtpVerification;
import com.example.chatApp.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByUserIdAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(
            Long userId, OtpPurpose purpose);

    void deleteByUserIdAndPurpose(Long userId, OtpPurpose purpose);
}
