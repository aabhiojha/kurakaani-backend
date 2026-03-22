package com.abhishekojha.kurakanimonolith.common.security;

import com.abhishekojha.kurakanimonolith.modules.user.AppUser;
import com.abhishekojha.kurakanimonolith.modules.user.UserRepository;
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

    public AppUser getRequestUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            log.warn("Security context does not contain authentication");
            return null;
        }

        String email = authentication.getName();
        log.debug("Looking up request user by email={}", email);

        AppUser user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.warn("No AppUser found for authenticated principal={}", email);
        } else {
            log.debug("Resolved request user: id={}, email={}", user.getId(), user.getEmail());
        }
        return user;
    }
}
