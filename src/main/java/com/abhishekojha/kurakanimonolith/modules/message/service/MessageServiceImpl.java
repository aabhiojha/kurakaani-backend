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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
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
        log.debug("event=send_text_message_attempt roomId={} user={}", roomId, principal.getName());

        if (request.getContent() == null || request.getContent().isBlank()) {
            log.warn("event=send_text_message_rejected reason=empty_content roomId={} user={}", roomId, principal.getName());
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
        log.info("event=message_saved message Id={} roomId={} userId={}", savedMessage.getId(), savedMessage.getRoom().getId(), savedMessage.getSender().getId());

        if (room.getType() == RoomType.DM) {
            String senderUsername = sender.getUsername();
            String receiverUsername = room.getMembers().stream()
                    .map(member -> member.getUser())
                    .filter(user -> !user.getId().equals(sender.getId()))
                    .map(User::getUsername)
                    .findFirst()
                    .orElseThrow();

            var dto = messageMapper.toDto(savedMessage);

            String receiverChannel = "chat.dm.user." + receiverUsername;
            String senderChannel = "chat.dm.user." + senderUsername;
            redisTemplate.convertAndSend(receiverChannel, dto);
            redisTemplate.convertAndSend(senderChannel, dto);
            log.info("event=dm_message_published messageId={} senderChannel={} receiverChannel={}", savedMessage.getId(), senderChannel, receiverChannel);
            return;
        }

        var dto = messageMapper.toDto(savedMessage);
        String groupChannel = "chat.group." + roomId;
        redisTemplate.convertAndSend(groupChannel, dto);
        log.info("event=group_message_published messageId={} channel={} userId={}", savedMessage.getId(), groupChannel, dto.getSenderId());
    }

    @Override
    @Transactional
    public MessageDto sendMediaMessageToRoom(Long roomId, MultipartFile file, String content, Principal principal) {
        log.debug("event=send_media_message_attempt roomId={} user={} contentType={} fileSize={}",
                roomId, principal.getName(), file != null ? file.getContentType() : "null", file != null ? file.getSize() : 0);

        if (file == null || file.isEmpty()) {
            log.warn("event=send_media_message_rejected reason=missing_file roomId={} user={}", roomId, principal.getName());
            throw new BadRequestException("Image or video file is required.");
        }

        MessageType messageType = resolveMessageType(file.getContentType());
        log.debug("event=media_type_resolved roomId={} messageType={}", roomId, messageType);

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
            log.info("event=media_uploaded roomId={} userId={} mediaKey={}", roomId, sender.getId(), mediaKey);
        } catch (Exception e) {
            log.error("event=media_upload_failed roomId={} userId={} folder={} error={}", roomId, sender.getId(), folder, e.getMessage(), e);
            throw new RuntimeException("Failed to upload media file", e);
        }

        Message savedMessage;
        try {
            savedMessage = saveMessage(room, sender, content, messageType, mediaKey, file.getContentType(), file.getOriginalFilename());
            log.info("event=media_message_saved messageId={} roomId={} userId={} mediaKey={}", savedMessage.getId(), roomId, sender.getId(), mediaKey);
        } catch (RuntimeException e) {
            log.error("event=media_message_save_failed roomId={} userId={} mediaKey={} error={} — rolling back upload", roomId, sender.getId(), mediaKey, e.getMessage());
            s3Operations.deleteFile(mediaKey);
            throw e;
        }

        MessageDto messageDto = messageMapper.toDto(savedMessage);

        if (room.getType() == RoomType.DM) {
            String senderUsername = sender.getUsername();
            String receiverUsername = room.getMembers().stream()
                    .map(member -> member.getUser())
                    .filter(user -> !user.getId().equals(sender.getId()))
                    .map(User::getUsername)
                    .findFirst()
                    .orElseThrow();

            redisTemplate.convertAndSend("chat.dm.user." + receiverUsername, messageDto);
            redisTemplate.convertAndSend("chat.dm.user." + senderUsername, messageDto);
            log.info("event=dm_media_message_published messageId={} senderUsername={} receiverUsername={}", savedMessage.getId(), senderUsername, receiverUsername);
        } else {
            String groupChannel = "chat.group." + roomId;
            redisTemplate.convertAndSend(groupChannel, messageDto);
            log.info("event=group_media_message_published messageId={} channel={} userId={}", savedMessage.getId(), groupChannel, sender.getId());
        }
        return messageDto;
    }

    @Override
    public List<MessageDto> searchMessagesInRoom(Long roomId, String searchText, Principal principal) {
        log.debug("event=search_messages_in_room roomId={} user={} query=\"{}\"", roomId, principal.getName(), searchText);
        getAuthorizedRoom(roomId, principal);
        List<MessageDto> results = messageRepository.fullTextSearchByRoom(roomId, searchText)
                .stream()
                .map(messageMapper::toDto)
                .toList();
        log.info("event=search_messages_in_room_done roomId={} user={} resultCount={}", roomId, principal.getName(), results.size());
        return results;
    }

    @Override
    public List<MessageDto> searchMessagesAcrossRooms(Principal principal, String searchText) {
        User user = getSender(principal);
        log.debug("event=search_messages_across_rooms userId={} query=\"{}\"", user.getId(), searchText);
        List<MessageDto> results = messageRepository.fullTextSearchAcrossRooms(searchText, user.getId())
                .stream()
                .map(messageMapper::toDto)
                .toList();
        log.info("event=search_messages_across_rooms_done userId={} resultCount={}", user.getId(), results.size());
        return results;
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
            log.warn("event=unauthorized_room_access roomId={} userId={}", roomId, sender.getId());
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
