package com.abhishekojha.kurakanimonolith.common.security;

import com.abhishekojha.kurakanimonolith.common.exception.exceptions.ResourceNotFoundException;
import com.abhishekojha.kurakanimonolith.common.exception.exceptions.UnauthorizedException;
import com.abhishekojha.kurakanimonolith.modules.user.model.User;
import com.abhishekojha.kurakanimonolith.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityUtils {

    private final UserRepository userRepository;

    public User getRequestUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Security context does not contain authentication");
            throw new UnauthorizedException("The user is not authenticated.");
        }

        String principal = authentication.getName();
        log.debug("Looking up request user by principal={}", principal);

        User user = userRepository.findByUserName(principal)
                .or(() -> userRepository.findByEmailIgnoreCase(principal))
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (user == null) {
            log.warn("No AppUser found for authenticated principal={}", principal);
        } else {
            log.debug("Resolved request user: id={}, username={}, email={}",
                    user.getId(), user.getUsername(), user.getEmail());
        }
        return user;
    }
}
