package com.abhishekojha.kurakanimonolith.auth;

import com.abhishekojha.kurakanimonolith.security.HttpCookieOAuth2AuthorizationRequestRepository;
import com.abhishekojha.kurakanimonolith.security.JwtService;
import com.abhishekojha.kurakanimonolith.user.AppUser;
import com.abhishekojha.kurakanimonolith.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        if (email == null || email.isBlank()) {
            throw new ServletException("OAuth2 provider did not return an email address");
        }

        AppUser user = userService.loadOrCreateOAuth2User(email, name);
        String token = jwtService.generateToken(user);
        authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
                "tokenType", "Bearer",
                "accessToken", token,
                "expiresAt", Instant.ofEpochMilli(jwtService.parseToken(token).getBody().getExpiration().getTime()),
                "user", Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "name", user.getName(),
                        "roles", user.getRoles()
                )
        ));
    }
}
