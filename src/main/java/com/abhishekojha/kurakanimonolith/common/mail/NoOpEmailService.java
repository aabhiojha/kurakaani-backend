package com.abhishekojha.kurakanimonolith.common.mail;

import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@ConditionalOnMissingBean(JavaMailSender.class)
public class NoOpEmailService implements EmailService {

    @Override
    public void sendPlainText(String to, String subject, String body) {
        log.warn("Skipping plain text email to {} because mail is not configured", to);
    }

    @Override
    public void sendHtml(String to, String subject, String templateName, Map<String, String> placeholders) throws MessagingException {
        log.warn(
                "Skipping HTML email to {} because mail is not configured. subject={}, template={}",
                to,
                subject,
                templateName
        );
    }
}
