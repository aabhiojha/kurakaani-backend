package com.abhishekojha.kurakanimonolith.common.mail;

import com.abhishekojha.kurakanimonolith.modules.auth.event.PasswordResetConfirmEvent;
import com.abhishekojha.kurakanimonolith.modules.auth.event.PasswordResetEvent;
import com.abhishekojha.kurakanimonolith.modules.auth.event.UserRegisteredEvent;
import com.abhishekojha.kurakanimonolith.modules.user.model.User;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailListener {
    private final EmailService emailService;

    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) throws MessagingException {
        User user = event.user();
        log.debug("The user {} has successfully registered", user.getUsername());
        emailService.sendHtml(
                user.getEmail(),
                "Welcome to the website",
                "welcome-email.html",
                Map.of("username", user.getUsername())
        );
    }

    @EventListener
    public void handlePasswordReset(PasswordResetEvent event) throws MessagingException {
        emailService.sendHtml(
                event.email(),
                "Password update request OTP",
                "password-reset.html",
                Map.of("RESET_CODE", String.valueOf(event.token()))
        );
        log.debug("Password reset code sent to {}", event.email());
    }

    @EventListener
    public void handlePasswordResetConfirmation(PasswordResetConfirmEvent event) throws MessagingException {
        User user = event.user();

        emailService.sendHtml(
                user.getEmail(),
                "Password reset successful",
                "password-reset-confimation.html",
                Map.of("username", user.getUsername())
        );
        log.debug("Password reset notice sent to email {}", user.getEmail());
    }

}
