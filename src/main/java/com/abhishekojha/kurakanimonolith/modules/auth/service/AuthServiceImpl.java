package com.abhishekojha.kurakanimonolith.modules.auth.service;

import com.abhishekojha.kurakanimonolith.common.helpers.RandomNumbers;
import com.abhishekojha.kurakanimonolith.common.security.JwtService;
import com.abhishekojha.kurakanimonolith.modules.auth.authDTO.*;
import com.abhishekojha.kurakanimonolith.modules.auth.event.PasswordResetConfirmEvent;
import com.abhishekojha.kurakanimonolith.modules.auth.event.PasswordResetEvent;
import com.abhishekojha.kurakanimonolith.modules.auth.event.UserRegisteredEvent;
import com.abhishekojha.kurakanimonolith.modules.auth.model.PasswordResetToken;
import com.abhishekojha.kurakanimonolith.modules.auth.model.Role;
import com.abhishekojha.kurakanimonolith.modules.auth.repository.PasswordResetTokenRepository;
import com.abhishekojha.kurakanimonolith.modules.auth.repository.RoleRepository;
import com.abhishekojha.kurakanimonolith.modules.user.model.User;
import com.abhishekojha.kurakanimonolith.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final ApplicationEventPublisher publisher;

    @Override
    @Async
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByUserName(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Create new user with encoded password
        User user = new User();
        user.setUserName(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);

        Role defaultRole = roleRepository.findByNameIgnoreCase("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));
        user.getRoles().add(defaultRole);

        // Save to database
        userRepository.save(user);

        // Generate JWT for immediate login after registration
        var jwt = jwtService.generateToken(user);

        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        publisher.publishEvent(new UserRegisteredEvent(user));
        new AuthResponse(jwt, user.getUsername(), roles);
    }

    public AuthResponse authenticate(AuthRequest request) {
        // Let Spring Security validate credentials
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // If we get here, credentials are valid
        var user = userRepository.findByUserName(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Generate and return JWT
        var jwt = jwtService.generateToken(user);
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .toList();
        return new AuthResponse(jwt, user.getUsername(), roles);
    }

    @Async
    @Transactional
    @Override
    public void password_reset(PasswordResetDTO passwordResetDTO) {
        String email = passwordResetDTO.getEmail();
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            log.debug("The requested user does not exist {}", email);
            return;
        }

//        check if the otp is already sent/ in the database
        Optional<PasswordResetToken> existingToken = passwordResetTokenRepository.findFirstByUserOrderByIdDesc(user);
        PasswordResetToken pst = existingToken.orElseGet(() -> PasswordResetToken.builder()
                .user(user)
                .build());

        pst.setToken(RandomNumbers.generateRandomNumbers());
        pst.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        pst.setUsed(false);

        PasswordResetToken save = passwordResetTokenRepository.save(pst);
        log.debug("The password reset token is saved: {}", save);
        publisher.publishEvent(new PasswordResetEvent(user.getEmail(), pst.getToken()));
    }

    @Async
    @Transactional
    @Override
    public void password_reset_confirm(PasswordResetConfirmDTO passwordResetConfirmDTO) {
        // get the token from password reset token table
        PasswordResetToken token = passwordResetTokenRepository.findByToken(passwordResetConfirmDTO.getToken());
        if (token == null) {
            log.debug("The token is not valid");
            return;
        }

        log.debug("The token is retrieved from db");

        if (token.getToken().equals(passwordResetConfirmDTO.getToken())) {
            log.debug("The token is valid");
            // update the user password to the provided password
            User user = token.getUser();
            String encodedPassword = passwordEncoder.encode(passwordResetConfirmDTO.getPassword());
            user.setPassword(encodedPassword);

            publisher.publishEvent(new PasswordResetConfirmEvent(user));
        } else {
            log.debug("Invalid Token");
        }
    }
}
