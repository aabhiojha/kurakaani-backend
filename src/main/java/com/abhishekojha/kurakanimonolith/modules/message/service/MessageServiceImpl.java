package com.abhishekojha.kurakanimonolith.modules.message.service;

import com.abhishekojha.kurakanimonolith.common.exception.exceptions.BadRequestException;
import com.abhishekojha.kurakanimonolith.common.exception.exceptions.ResourceNotFoundException;
import com.abhishekojha.kurakanimonolith.common.exception.exceptions.UnauthorizedException;
import com.abhishekojha.kurakanimonolith.common.objectStorage.S3Operations;
import com.abhishekojha.kurakanimonolith.common.security.SecurityUtils;
import com.abhishekojha.kurakanimonolith.modules.message.dto.MessageDto;
import com.abhishekojha.kurakanimonolith.modules.message.dto.MessageRequest;
import com.abhishekojha.kurakanimonolith.modules.message.mapper.MessageMapper;
import com.abhishekojha.kurakanimonolith.modules.message.model.Message;
import com.abhishekojha.kurakanimonolith.modules.message.model.MessageType;
import com.abhishekojha.kurakanimonolith.modules.message.repository.MessageRepository;
import com.abhishekojha.kurakanimonolith.modules.room.model.Room;
import com.abhishekojha.kurakanimonolith.modules.room.model.RoomType;
import com.abhishekojha.kurakanimonolith.modules.room.repository.RoomRepository;
import com.abhishekojha.kurakanimonolith.modules.room_member.repository.RoomMemberRepository;
import com.abhishekojha.kurakanimonolith.modules.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final MessageMapper messageMapper;
    private final S3Operations s3Operations;
    private final SecurityUtils securityUtils;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public void sendMessageToRoom(Long roomId, MessageRequest request, Principal principal) {
            if (request.getContent() == null || request.getContent().isBlank()) {
                throw new BadRequestException("Message content is required.");
            }

            Room room = getAuthorizedRoom(roomId, principal);
            User sender = getSender(principal);

            Message savedMessage = saveMessage(
                    room,
                    sender,
                    request.getContent(),
                    MessageType.TEXT,
                    null,
                    null,
                    null
            );


        if (room.getType() == RoomType.DM) {
            Long senderId = sender.getId();
            Long receiverId = room.getMembers().stream()
                    .map(member -> member.getUser().getId())
                    .filter(id -> !id.equals(senderId))
                    .findFirst()
                    .orElseThrow();

            var dto = messageMapper.toDto(savedMessage);

            // Send to receiver
            redisTemplate.convertAndSend(
                    "chat.dm.user." + receiverId,
                    dto
            );

            // Send to sender (so sender also sees message in real-time)
            redisTemplate.convertAndSend(
                    "chat.dm.user." + senderId,
                    dto
            );
            return;
        }
        redisTemplate.convertAndSend(
                    "chat.group." + roomId,
                    messageMapper.toDto(savedMessage)
        );

    }

    @Override
    @Transactional
    public MessageDto sendMediaMessageToRoom(Long roomId, MultipartFile file, String content, Principal principal) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image or video file is required.");
        }

        MessageType messageType = resolveMessageType(file.getContentType());
        Room room = getAuthorizedRoom(roomId, principal);
        User sender = getSender(principal);

        String folder = switch (messageType) {
            case IMAGE -> "chat/group/" + roomId + "/images";
            case VIDEO -> "chat/group/" + roomId + "/videos";
            default -> throw new BadRequestException("Unsupported message type.");
        };

        String mediaKey;
        try {
            mediaKey = s3Operations.uploadFile(file, folder);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload media file", e);
        }

        Message savedMessage;
        try {
            savedMessage = saveMessage(room, sender, content, messageType, mediaKey, file.getContentType(), file.getOriginalFilename());
        } catch (RuntimeException e) {
            s3Operations.deleteFile(mediaKey);
            throw e;
        }

        MessageDto messageDto = messageMapper.toDto(savedMessage);

        if (room.getType() == RoomType.DM) {
            Long senderId = sender.getId();
            Long receiverId = room.getMembers().stream()
                    .map(member -> member.getUser().getId())
                    .filter(id -> !id.equals(senderId))
                    .findFirst()
                    .orElseThrow();

            // Send to receiver
            redisTemplate.convertAndSend(
                    "chat.dm.user." + receiverId,
                    messageDto
            );

            // Send to sender (so sender also sees message in real-time)
            redisTemplate.convertAndSend(
                    "chat.dm.user." + senderId,
                    messageDto
            );
        } else {
            redisTemplate.convertAndSend("chat.group." + roomId, messageDto);
        }
        return messageDto;
    }

    @Override
    public List<MessageDto> searchMessagesInRoom(Long roomId, String searchText, Principal principal) {
        getAuthorizedRoom(roomId, principal);
        return messageRepository.fullTextSearchByRoom(roomId, searchText)
                .stream()
                .map(messageMapper::toDto)
                .toList();
    }

    @Override
    public List<MessageDto> searchMessagesAcrossRooms(Principal principal, String searchText) {
        User user = getSender(principal);
        return messageRepository.fullTextSearchAcrossRooms(searchText, user.getId())
                .stream()
                .map(messageMapper::toDto)
                .toList();
    }


    private Message saveMessage(Room room, User sender, String content, MessageType messageType, String mediaKey, String mediaContentType, String mediaFileName) {
        return messageRepository.save(Message.builder()
                .sender(sender)
                .room(room)
                .content(content)
                .messageType(messageType)
                .mediaKey(mediaKey)
                .mediaContentType(mediaContentType)
                .mediaFileName(mediaFileName)
                .isEdited(Boolean.FALSE)
                .isDeleted(Boolean.FALSE)
                .build());
    }

    private Room getAuthorizedRoom(Long roomId, Principal principal) {
        Room room = roomRepository.findById(roomId).orElseThrow(
                () -> new ResourceNotFoundException("Room not found")
        );

        User sender = getSender(principal);
        boolean isMember = roomMemberRepository.existsByRoomIdAndUserId(roomId, sender.getId());
        if (!isMember) {
            throw new UnauthorizedException("You are not a member of this room");
        }
        return room;
    }

    private User getSender(Principal principal) {
        return securityUtils.getRequestUser(principal);
    }

    private MessageType resolveMessageType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new BadRequestException("Unsupported file type.");
        }

        if (contentType.startsWith("image/")) {
            return MessageType.IMAGE;
        }

        if (contentType.startsWith("video/")) {
            return MessageType.VIDEO;
        }

        throw new BadRequestException("Only image and video files are supported.");
    }
}
