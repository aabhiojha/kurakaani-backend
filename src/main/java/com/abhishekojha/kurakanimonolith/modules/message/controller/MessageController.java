package com.abhishekojha.kurakanimonolith.modules.message.controller;

import com.abhishekojha.kurakanimonolith.modules.message.TypingEvent;
import com.abhishekojha.kurakanimonolith.modules.message.dto.MessageDto;
import com.abhishekojha.kurakanimonolith.modules.message.dto.MessageRequest;
import com.abhishekojha.kurakanimonolith.modules.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@CrossOrigin("*")
public class MessageController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send/{roomId}")
    public void sendMessageToRoom(
            @DestinationVariable Long roomId,
            @Payload MessageRequest request,
            Principal principal
    ) {
        messageService.sendMessageToRoom(roomId, request, principal);
    }

    @PostMapping("/api/rooms/room/{roomId}/message/media")
    @ResponseBody
    public ResponseEntity<MessageDto> uploadMediaToRoom(
            @PathVariable Long roomId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "content", required = false) String content,
            Principal principal
    ) {
        return new ResponseEntity<>(messageService.sendMediaMessageToRoom(roomId, file, content, principal), HttpStatus.CREATED);
    }

    @MessageMapping("/chat.typing/{roomId}")
    public void handleTyping(@DestinationVariable Long roomId,
                             @Payload TypingEvent event,
                             Principal principal){
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/typing", event);

    }
}
