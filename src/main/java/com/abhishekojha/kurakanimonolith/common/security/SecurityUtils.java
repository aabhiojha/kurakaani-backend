package com.abhishekojha.kurakanimonolith.common.security;

import com.abhishekojha.kurakanimonolith.modules.user.AppUser;
import com.abhishekojha.kurakanimonolith.modules.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    public AppUser getRequestUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email).orElse(null);
    }
}
