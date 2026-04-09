package com.example.chatApp.config;

import com.example.chatApp.dto.AuthResponse;
import com.example.chatApp.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;

    public OAuth2AuthenticationSuccessHandler(@Lazy AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        // Process user and generate JWT tokens
        AuthResponse authResponse = authService.processOAuth2User(email, name, picture);

        // Redirect to frontend with tokens as query params
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:5173/auth/callback")
                .queryParam("token", authResponse.getToken())
                .queryParam("refreshToken", authResponse.getRefreshToken())
                .queryParam("userId", authResponse.getUserId())
                .queryParam("username", authResponse.getUsername())
                .queryParam("email", authResponse.getEmail())
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
