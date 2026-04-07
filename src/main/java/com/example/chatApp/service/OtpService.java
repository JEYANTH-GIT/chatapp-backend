package com.example.chatApp.service;

import com.example.chatApp.model.OtpVerification;
import com.example.chatApp.model.User;
import com.example.chatApp.enums.OtpPurpose;
import com.example.chatApp.repository.OtpVerificationRepository;
import com.example.chatApp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;

    private final OtpVerificationRepository otpRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom;

    public OtpService(OtpVerificationRepository otpRepository,
                      UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      EmailService emailService) {
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.secureRandom = new SecureRandom();
    }

    @Transactional
    public String generateAndSendOtp(User user, OtpPurpose purpose) {
        String otp = generateOtp();
        String otpHash = passwordEncoder.encode(otp);

        OtpVerification otpVerification = OtpVerification.builder()
                .userId(user.getId())
                .otpHash(otpHash)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .isUsed(false)
                .build();

        otpRepository.save(otpVerification);
        emailService.sendOtpEmail(user.getEmail(), otp, purpose.name());
        log.info("OTP generated and sent for user: {} purpose: {}", user.getEmail(), purpose);
        return otp;
    }

    @Transactional
    public boolean verifyOtp(String email, String otp, OtpPurpose purpose) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        Optional<OtpVerification> otpOpt = otpRepository
                .findTopByUserIdAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(user.getId(), purpose);

        if (otpOpt.isEmpty()) {
            log.warn("No active OTP found for user: {} purpose: {}", email, purpose);
            return false;
        }

        OtpVerification otpVerification = otpOpt.get();

        if (otpVerification.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("OTP expired for user: {}", email);
            return false;
        }

        if (!passwordEncoder.matches(otp, otpVerification.getOtpHash())) {
            log.warn("Invalid OTP for user: {}", email);
            return false;
        }

        otpVerification.setIsUsed(true);
        otpRepository.save(otpVerification);

        if (purpose == OtpPurpose.REGISTRATION) {
            user.setIsVerified(true);
            userRepository.save(user);
        }

        log.info("OTP verified successfully for user: {} purpose: {}", email, purpose);
        return true;
    }

    @Transactional
    public void resendOtp(String email, OtpPurpose purpose) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        generateAndSendOtp(user, purpose);
    }

    private String generateOtp() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(secureRandom.nextInt(10));
        }
        return otp.toString();
    }
}
