package com.abhishekojha.kurakanimonolith.modules.notification.service;

import com.abhishekojha.kurakanimonolith.modules.notification.NotificationMessage;
import com.abhishekojha.kurakanimonolith.modules.notification.enums.NotificationType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@AllArgsConstructor
public class NotificationService {

    private static final String CHANNEL_PREFIX = "notifications.";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public <T> void notify(String recipientUsername, NotificationType type, T payload) {
        NotificationMessage<T> message = NotificationMessage.<T>builder()
                .id(UUID.randomUUID().toString())
                .type(type)
                .timestamp(Instant.now())
                .payload(payload)
                .build();

        try {
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(CHANNEL_PREFIX + recipientUsername, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize notification", e);
        }
    }
}
