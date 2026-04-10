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
        log.debug("event=redis_message_received channel={}", channel);

        // DM (user-specific)
        if (channel.startsWith("chat.dm.user.")) {
            String userId = extractUserId(channel);
            messagingTemplate.convertAndSendToUser(userId, "/queue/messages", message);
            log.debug("event=dm_message_forwarded userId={}", userId);
            return;
        }

        // Group chat (broadcast)
        if (channel.startsWith("chat.group.")) {
            messagingTemplate.convertAndSend("/topic/" + channel, message);
            log.debug("event=group_message_forwarded channel={}", channel);
            return;
        }

        // Typing events
        if (channel.startsWith("chat.typing.")) {
            String roomId = extractUserId(channel);
            messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/typing", message);
            log.debug("event=typing_event_forwarded roomId={}", roomId);
            return;
        }

        // Notifications
        if (channel.startsWith("notification.")) {
            String userId = extractUserId(channel);
            messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", message);
            log.debug("event=notification_forwarded userId={}", userId);
        }
    }

    private String extractUserId(String channel) {
        return channel.substring(channel.lastIndexOf(".") + 1);
    }
}