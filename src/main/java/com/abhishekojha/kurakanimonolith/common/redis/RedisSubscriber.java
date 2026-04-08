package com.abhishekojha.kurakanimonolith.common.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSubscriber {

    private final SimpMessagingTemplate messagingTemplate;

    public void onMessage(Object message, String channel) {
        log.debug("onMessage called with message: {}, channel: {}", message, channel); // Added

        // DM (user-specific)
        if (channel.startsWith("chat.dm.user.")) {

            String userId = extractUserId(channel);
            log.debug("The chat dm user of id: {} is subscribed", userId);
            messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/messages",
                    message
            );
            return;
        }

        // Group chat (broadcast)
        if (channel.startsWith("chat.group.")) {
            messagingTemplate.convertAndSend(
                    "/topic/" + channel,
                    message
            );
            return;
        }

        // Typing events
        if (channel.startsWith("chat.typing.")) {
            String roomId = extractUserId(channel);
            messagingTemplate.convertAndSend(
                    "/topic/rooms/" + roomId + "/typing",
                    message
            );
            return;
        }

        // Notifications
        if (channel.startsWith("notification.")) {

            String userId = extractUserId(channel);

            messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/notifications",
                    message
            );
        }
    }

    private String extractUserId(String channel) {
        return channel.substring(channel.lastIndexOf(".") + 1);
    }
}