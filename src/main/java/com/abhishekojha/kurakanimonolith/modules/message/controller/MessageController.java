package com.abhishekojha.kurakanimonolith.modules.message.controller;

import com.abhishekojha.kurakanimonolith.common.exception.exceptions.ResourceNotFoundException;
import com.abhishekojha.kurakanimonolith.common.exception.exceptions.UnauthorizedException;
import com.abhishekojha.kurakanimonolith.common.security.SecurityUtils;
import com.abhishekojha.kurakanimonolith.modules.message.dto.MessageRequest;
import com.abhishekojha.kurakanimonolith.modules.message.mapper.MessageMapper;
import com.abhishekojha.kurakanimonolith.modules.message.model.Message;
import com.abhishekojha.kurakanimonolith.modules.message.repository.MessageRepository;
import com.abhishekojha.kurakanimonolith.modules.room.model.Room;
import com.abhishekojha.kurakanimonolith.modules.room.repository.RoomRepository;
import com.abhishekojha.kurakanimonolith.modules.room_member.repository.RoomMemberRepository;
import com.abhishekojha.kurakanimonolith.modules.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDateTime;

//@RestController
//@RequestMapping("/api/messages")

@Controller
@RequiredArgsConstructor
@CrossOrigin("*")
public class MessageController {
    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final SecurityUtils securityUtils;
    private final RoomMemberRepository roomMemberRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageMapper messageMapper;

    @MessageMapping("/chat.send/{roomId}")
//    @SendTo("/topic/room/{roomId}")             // subscribe
    public void sendMessage(
            @DestinationVariable Long roomId,
            @RequestBody MessageRequest request,
            Principal principal
    ) {
        Room room = roomRepository.findById(roomId).orElseThrow(
                () -> new ResourceNotFoundException("The room not found")
        );
        User sender = securityUtils.getRequestUser();

        boolean isMember = roomMemberRepository.existsByRoomIdAndUserId(roomId, sender.getId());
        if (!isMember) {
            throw new UnauthorizedException("You are not a member of this room");
        }

        Message message = Message.builder()
                .sender(sender)
                .room(room)
                .content(request.getContent())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Message savedMessage = messageRepository.save(message);

        room.getMessages().add(savedMessage);

        messagingTemplate.convertAndSend(
                "/topic/rooms/" + roomId,
                messageMapper.toDto(savedMessage)
        );
    }
}
