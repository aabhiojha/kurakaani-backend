package com.abhishekojha.kurakanimonolith.modules.message.controller;

import com.abhishekojha.kurakanimonolith.common.exception.exceptions.ResourceNotFoundException;
import com.abhishekojha.kurakanimonolith.common.security.SecurityUtils;
import com.abhishekojha.kurakanimonolith.modules.message.dto.MessageRequest;
import com.abhishekojha.kurakanimonolith.modules.message.model.Message;
import com.abhishekojha.kurakanimonolith.modules.message.repository.MessageRepository;
import com.abhishekojha.kurakanimonolith.modules.room.model.Room;
import com.abhishekojha.kurakanimonolith.modules.room.repository.RoomRepository;
import com.abhishekojha.kurakanimonolith.modules.user.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

//@RestController
//@RequestMapping("/api/messages")

@Controller
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173")
public class MessageController {
    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final SecurityUtils securityUtils;

    @MessageMapping("/sendMessage/{roomId}")     // /app/sendMesage/roomId
    @SendTo("/topic/room/{roomId}")             // subscribe
    public Message sendMessage(
            @DestinationVariable Long roomId,
            @RequestBody MessageRequest request
    ) {
        Room room = roomRepository.findById(roomId).orElseThrow(
                () -> new ResourceNotFoundException("The room not found")
        );
        AppUser user = securityUtils.getRequestUser();

        Message message = Message.builder()
                .sender(user)
                .room(room)
                .content(request.getContent())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Message savedMessage = messageRepository.save(message);

        room.getMessages().add(savedMessage);
        return message;
    }
}
