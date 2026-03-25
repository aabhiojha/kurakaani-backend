package com.abhishekojha.kurakanimonolith.modules.message.service;

import com.abhishekojha.kurakanimonolith.common.exception.exceptions.ResourceNotFoundException;
import com.abhishekojha.kurakanimonolith.common.exception.exceptions.UnauthorizedException;
import com.abhishekojha.kurakanimonolith.modules.message.dto.MessageRequest;
import com.abhishekojha.kurakanimonolith.modules.message.mapper.MessageMapper;
import com.abhishekojha.kurakanimonolith.modules.message.model.Message;
import com.abhishekojha.kurakanimonolith.modules.message.repository.MessageRepository;
import com.abhishekojha.kurakanimonolith.modules.room.model.Room;
import com.abhishekojha.kurakanimonolith.modules.room.repository.RoomRepository;
import com.abhishekojha.kurakanimonolith.modules.room_member.repository.RoomMemberRepository;
import com.abhishekojha.kurakanimonolith.modules.user.model.User;
import com.abhishekojha.kurakanimonolith.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.security.Principal;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageMapper messageMapper;
    private final UserRepository userRepository;


    @Override
    public void sendMessage(Long roomId, MessageRequest request, Principal principal) {
        Room room = roomRepository.findById(roomId).orElseThrow(
                () -> new ResourceNotFoundException("Room not found")
        );
        User sender = userRepository.findByUserName(principal.getName()).orElseThrow(
                () -> new ResourceNotFoundException("User not found")
        );

        boolean isMember = roomMemberRepository.existsByRoomIdAndUserId(roomId, sender.getId());
        if (!isMember) {
            throw new UnauthorizedException("You are not a member of this room");
        }

        Message savedMessage = messageRepository.save(Message.builder()
                .sender(sender)
                .room(room)
                .content(request.getContent())
                .isEdited(Boolean.FALSE)
                .isDeleted(Boolean.FALSE)
                .build());

        messagingTemplate.convertAndSend(
                "/topic/rooms/" + roomId,
                messageMapper.toDto(savedMessage)
        );
    }
}
