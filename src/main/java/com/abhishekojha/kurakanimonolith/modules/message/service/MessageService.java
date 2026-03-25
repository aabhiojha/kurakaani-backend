package com.abhishekojha.kurakanimonolith.modules.message.service;

import com.abhishekojha.kurakanimonolith.modules.message.dto.MessageRequest;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Payload;

import java.security.Principal;

public interface MessageService {
    void sendMessageToRoom(
            Long roomId,
            MessageRequest request,
            Principal principal
    );
}
