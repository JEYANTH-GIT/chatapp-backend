package com.example.chatApp.service;

import com.example.chatApp.dto.*;
import com.example.chatApp.entity.User;
import com.example.chatApp.enums.AuthProvider;
import com.example.chatApp.enums.OtpPurpose;
import com.example.chatApp.repository.UserRepository;
import com.example.chatApp.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final OtpService otpService;
    private final PasswordPolicyService passwordPolicyService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager,
                       OtpService otpService,
                       PasswordPolicyService passwordPolicyService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.otpService = otpService;
        this.passwordPolicyService = passwordPolicyService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already registered");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username is already taken");
        }

        List<String> passwordErrors = passwordPolicyService.validate(request.getPassword());
        if (!passwordErrors.isEmpty()) {
            throw new RuntimeException("Password policy violation: " + String.join(", ", passwordErrors));
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .authProvider(AuthProvider.LOCAL)
                .isVerified(false)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());

        otpService.generateAndSendOtp(user, OtpPurpose.REGISTRATION);

        return AuthResponse.builder()
                .message("Registration successful. Please verify your email with the OTP sent.")
                .email(user.getEmail())
                .username(user.getUsername())
                .verified(false)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getIsVerified()) {
            otpService.generateAndSendOtp(user, OtpPurpose.REGISTRATION);
            return AuthResponse.builder()
                    .message("Account not verified. OTP has been sent to your email.")
                    .email(user.getEmail())
                    .verified(false)
                    .build();
        }

        String accessToken = jwtUtil.generateToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        log.info("User logged in successfully: {}", user.getEmail());

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .email(user.getEmail())
                .message("Login successful")
                .verified(true)
                .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        String userEmail = jwtUtil.extractUsername(refreshToken);
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                userEmail, "", List.of()
        );

        if (!jwtUtil.isTokenValid(refreshToken, userDetails)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        String newAccessToken = jwtUtil.generateToken(userDetails);

        return AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(refreshToken)
                .message("Token refreshed successfully")
                .build();
    }

    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        otpService.generateAndSendOtp(user, OtpPurpose.PASSWORD_RESET);
        log.info("Password reset OTP sent to: {}", email);
    }

    @Transactional
    public AuthResponse resetPassword(String email, String otp, String newPassword) {
        List<String> passwordErrors = passwordPolicyService.validate(newPassword);
        if (!passwordErrors.isEmpty()) {
            throw new RuntimeException("Password policy violation: " + String.join(", ", passwordErrors));
        }

        boolean otpValid = otpService.verifyOtp(email, otp, OtpPurpose.PASSWORD_RESET);
        if (!otpValid) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password reset successful for: {}", email);

        return AuthResponse.builder()
                .message("Password reset successful. Please login with your new password.")
                .email(email)
                .build();
    }

    @Transactional
    public AuthResponse processOAuth2User(String email, String name, String picture) {
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .username(name != null ? name.replaceAll("\\s+", "_").toLowerCase() : email.split("@")[0])
                    .email(email)
                    .authProvider(AuthProvider.GOOGLE)
                    .isVerified(true)
                    .profilePicture(picture)
                    .build();
            return userRepository.save(newUser);
        });

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(), "", List.of()
        );

        String accessToken = jwtUtil.generateToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);
        log.info("OAuth2 user processed: {}", email);

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .email(user.getEmail())
                .message("OAuth2 login successful")
                .verified(true)
                .build();
    }
}
