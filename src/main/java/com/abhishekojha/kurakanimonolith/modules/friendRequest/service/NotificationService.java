package com.abhishekojha.kurakanimonolith.modules.friendRequest.service;

import com.abhishekojha.kurakanimonolith.modules.friendRequest.dto.FriendShipDto;
import com.abhishekojha.kurakanimonolith.modules.friendRequest.dto.WebSocketNotification;
import com.abhishekojha.kurakanimonolith.modules.friendRequest.model.enums.WsResponseType;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

// 10th fucking rewrite of this class

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String NOTIFICATIONS_QUEUE = "/queue/notifications";

    private final SimpMessagingTemplate messagingTemplate;

    public void sendFriendRequestNotification(String recipientUsername, FriendShipDto friendShipDto) {
        send(recipientUsername, WsResponseType.FRIEND_REQUEST_RECEIVED, friendShipDto);
    }

    public void sendFriendRequestAccepted(String requesterUsername, FriendShipDto friendShipDto) {
        send(requesterUsername, WsResponseType.FRIEND_REQUEST_ACCEPTED, friendShipDto);
    }

    public void sendFriendRequestRejected(String requesterUsername, FriendShipDto friendShipDto) {
        send(requesterUsername, WsResponseType.FRIEND_REQUEST_REJECTED, friendShipDto);
    }

    public void sendFriendRemoved(String targetUsername, FriendShipDto friendShipDto) {
        send(targetUsername, WsResponseType.FRIEND_REMOVED, friendShipDto);
    }

    private void send(String username, WsResponseType type, Object payload) {
        messagingTemplate.convertAndSendToUser(
                username,
                NOTIFICATIONS_QUEUE,
                new WebSocketNotification(type, payload)
        );
    }
}
