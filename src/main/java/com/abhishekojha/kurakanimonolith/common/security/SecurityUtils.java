package com.abhishekojha.kurakanimonolith.common.security;

import com.abhishekojha.kurakanimonolith.common.exception.exceptions.ResourceNotFoundException;
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
        if (authentication == null) {
            log.warn("Security context does not contain authentication");
            return null;
        }

        String email = authentication.getName();
        log.debug("Looking up request user by email={}", email);

        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow(() ->
                new ResourceNotFoundException("User not found."));

        if (user == null) {
            log.warn("No AppUser found for authenticated principal={}", email);
        } else {
            log.debug("Resolved request user: id={}, email={}", user.getId(), user.getEmail());
        }
        return user;
    }
}
