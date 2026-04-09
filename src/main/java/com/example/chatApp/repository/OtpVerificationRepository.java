package com.example.chatApp.repository;

<<<<<<< HEAD
=======
import com.example.chatApp.model.OtpVerification;
>>>>>>> 336049e9327ef3bc762643b5dee206ef27479048
import com.example.chatApp.enums.OtpPurpose;
import com.example.chatApp.model.OtpVerification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByUserIdAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(
            Long userId, OtpPurpose purpose);

    void deleteByUserIdAndPurpose(Long userId, OtpPurpose purpose);
}
