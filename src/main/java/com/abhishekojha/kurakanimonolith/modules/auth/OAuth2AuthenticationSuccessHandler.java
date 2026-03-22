package com.abhishekojha.kurakanimonolith.modules.auth;

import com.abhishekojha.kurakanimonolith.common.security.HttpCookieOAuth2AuthorizationRequestRepository;
import com.abhishekojha.kurakanimonolith.common.security.JwtService;
import com.abhishekojha.kurakanimonolith.modules.user.AppUser;
import com.abhishekojha.kurakanimonolith.modules.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
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
        log.info("OAuth2 authentication succeeded for path={}", request.getRequestURI());

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        log.debug("OAuth2 principal attributes resolved: email={}, name={}", email, name);

        if (email == null || email.isBlank()) {
            log.warn("OAuth2 provider did not return an email address");
            throw new ServletException("OAuth2 provider did not return an email address");
        }

        AppUser user = userService.loadOrCreateOAuth2User(email, name);
        log.info("OAuth2 user resolved: id={}, email={}", user.getId(), user.getEmail());
        String token = jwtService.generateToken(user);
        authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
        log.debug("Authorization request cookies cleared for email={}", email);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        log.info("Returning OAuth2 login response for user={}", user.getEmail());
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
