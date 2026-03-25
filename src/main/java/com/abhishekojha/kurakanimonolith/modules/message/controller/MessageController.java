package com.abhishekojha.kurakanimonolith.modules.message.controller;

import com.abhishekojha.kurakanimonolith.modules.message.dto.MessageRequest;
import com.abhishekojha.kurakanimonolith.modules.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@CrossOrigin("*")
public class MessageController {

    private final MessageService messageService;

    @MessageMapping("/chat.send/{roomId}")
    public void sendMessageToRoom(
            @DestinationVariable Long roomId,
            @Payload MessageRequest request,
            Principal principal
    ) {
        messageService.sendMessageToRoom(roomId, request, principal);
    }
}
