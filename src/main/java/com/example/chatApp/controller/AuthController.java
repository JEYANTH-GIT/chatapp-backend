package com.example.chatApp.controller;

import com.example.chatApp.dto.*;
import com.example.chatApp.enums.OtpPurpose;
import com.example.chatApp.service.AuthService;
import com.example.chatApp.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication and account management endpoints")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    public AuthController(AuthService authService, OtpService otpService) {
        this.authService = authService;
        this.otpService = otpService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Register with email + password. An OTP will be sent to verify the email.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registration successful, OTP sent"),
            @ApiResponse(responseCode = "400", description = "Invalid request or email already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate with email + password. Returns JWT access and refresh tokens.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful, JWT returned"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Account not verified")
    })
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        }
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP", description = "Verify the OTP code sent to email for registration, login, or password reset.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired OTP")
    })
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        try {
            OtpPurpose purpose = OtpPurpose.valueOf(request.getPurpose().toUpperCase());
            boolean verified = otpService.verifyOtp(request.getEmail(), request.getOtp(), purpose);
            if (verified) {
                return ResponseEntity.ok(Map.of("message", "OTP verified successfully", "verified", true));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired OTP", "verified", false));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend OTP", description = "Resend OTP to the user's email for a given purpose.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP resent successfully"),
            @ApiResponse(responseCode = "400", description = "User not found or error sending OTP")
    })
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String purpose = request.getOrDefault("purpose", "REGISTRATION");
            otpService.resendOtp(email, OtpPurpose.valueOf(purpose.toUpperCase()));
            return ResponseEntity.ok(Map.of("message", "OTP has been resent to your email"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Logout and invalidate the current session.")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Logged out successfully") })
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh JWT token", description = "Issue a new access token using a valid refresh token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        try {
            AuthResponse response = authService.refreshToken(request.get("refreshToken"));
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Send a password reset OTP to the user's email.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset OTP sent"),
            @ApiResponse(responseCode = "400", description = "User not found")
    })
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            authService.forgotPassword(request.get("email"));
            return ResponseEntity.ok(Map.of("message", "Password reset OTP has been sent to your email"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Reset password using OTP and new password.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset successful"),
            @ApiResponse(responseCode = "400", description = "Invalid OTP or password policy violation")
    })
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            AuthResponse response = authService.resetPassword(
                    request.get("email"), request.get("otp"), request.get("newPassword"));
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/oauth2/google")
    @Operation(summary = "Google OAuth2 login", description = "Redirect to Google OAuth2 consent screen.")
    @ApiResponses({ @ApiResponse(responseCode = "302", description = "Redirect to Google") })
    public ResponseEntity<?> googleLogin() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/oauth2/authorization/google").build();
    }

    @GetMapping("/oauth2/callback")
    @Operation(summary = "OAuth2 callback", description = "Handle Google OAuth2 callback and return JWT tokens.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OAuth2 login successful"),
            @ApiResponse(responseCode = "400", description = "OAuth2 authentication failed")
    })
    public ResponseEntity<?> oauth2Callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error) {
        if (error != null) {
            return ResponseEntity.badRequest().body(Map.of("error", "OAuth2 authentication failed: " + error));
        }
        return ResponseEntity.ok(Map.of("message", "OAuth2 callback received. Use the token from redirect."));
    }
}
