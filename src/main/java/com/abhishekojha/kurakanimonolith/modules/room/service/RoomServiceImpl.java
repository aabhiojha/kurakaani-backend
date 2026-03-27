package com.abhishekojha.kurakanimonolith.modules.room.service;

import com.abhishekojha.kurakanimonolith.common.exception.exceptions.BadRequestException;
import com.abhishekojha.kurakanimonolith.common.exception.exceptions.DuplicateResourceException;
import com.abhishekojha.kurakanimonolith.common.exception.exceptions.ResourceNotFoundException;
import com.abhishekojha.kurakanimonolith.common.exception.exceptions.UnauthorizedException;
import com.abhishekojha.kurakanimonolith.common.objectStorage.S3Operations;
import com.abhishekojha.kurakanimonolith.modules.message.model.MessageType;
import com.abhishekojha.kurakanimonolith.common.security.SecurityUtils;
import com.abhishekojha.kurakanimonolith.modules.room.dto.*;
import com.abhishekojha.kurakanimonolith.modules.room.dto.roomList.RecentMessageDto;
import com.abhishekojha.kurakanimonolith.modules.room.dto.roomList.RoomListDto;
import com.abhishekojha.kurakanimonolith.modules.room.dto.roomMessage.RoomMessageDto;
import com.abhishekojha.kurakanimonolith.modules.room.mapper.RoomMapper;
import com.abhishekojha.kurakanimonolith.modules.room.model.Room;
import com.abhishekojha.kurakanimonolith.modules.room.model.RoomType;
import com.abhishekojha.kurakanimonolith.modules.room.repository.RoomRepository;
import com.abhishekojha.kurakanimonolith.modules.room_member.dto.RoomMemberDto;
import com.abhishekojha.kurakanimonolith.modules.room_member.mapper.RoomMemberMapper;
import com.abhishekojha.kurakanimonolith.modules.room_member.model.RoomMember;
import com.abhishekojha.kurakanimonolith.modules.room_member.model.RoomRole;
import com.abhishekojha.kurakanimonolith.modules.room_member.repository.RoomMemberRepository;
import com.abhishekojha.kurakanimonolith.modules.user.model.User;
import com.abhishekojha.kurakanimonolith.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomMemberMapper roomMemberMapper;
    private final S3Operations s3Operations;

    @Override
    @Transactional
    public List<RoomMemberDto> getAllMembers(Long roomId) {
        // get the room first
        Room room = roomRepository.findById(roomId).orElseThrow(
                () -> new ResourceNotFoundException("Room not found"));

        return roomMemberMapper.toDto(room.getMembers());
    }

    @Override
    @Transactional
    public RoomDto createRoomGroup(CreateRoomRequestDto createRoomRequestDto) {
        // user
        User user = securityUtils.getRequestUser();
        if (user == null) {
            throw new UnauthorizedException("The user not authenticated.");
        }
        // check if the room of same name exists already.
        List<Room> roomList = roomRepository.findByCreatedByAndNameIgnoreCase(user.getId(), createRoomRequestDto.getName());

        if (!roomList.isEmpty()) {
            throw new DuplicateResourceException("The room with name '%s' already exists".formatted(createRoomRequestDto.getName()));
        }

        log.info("The request user is retrieved: {}", user.getEmail());

        // convert the createroomdto into room object
        Room room = new Room();
        room.setName(createRoomRequestDto.getName());
        room.setDescription(createRoomRequestDto.getDescription());
        room.setType(createRoomRequestDto.getType());
        room.setCreatedBy(user);
//        room.setMembers();

        Room savedRoom = roomRepository.save(room);
        log.info("Room created. id={}, name={}, createdBy={}",
                savedRoom.getId(), savedRoom.getName(), user.getEmail());

        // gotta create a room member entry of the user as admin
        RoomMember roomMember = RoomMember.builder()
                .room(savedRoom)
                .user(user)
                .roomRole(RoomRole.ADMIN)
                .joinedAt(LocalDateTime.now()).build();

        RoomMember adminMember = roomMemberRepository.save(roomMember);
        log.info("User {} is set as admin of room {}", adminMember.getUser(), room);

//        now attach the member list to savedRoom
        savedRoom.setMembers(List.of(roomMember));

        return roomMapper.toDto(savedRoom);
    }

    @Override
    @Transactional
    public RoomDto createRoomDm(
            Long userId
    ) {
        User user1 = securityUtils.getRequestUser();

//        find the other user
        User user2 = userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("User not found")
        );

        // check if the two users are in some dm already
        Optional<Room> existingDm = roomRepository.findExistingDm(user1.getId(), user2.getId());
        if (existingDm.isPresent()) {
            return roomMapper.toDto(existingDm.get());
        }

        // create a new dm with these two members
        Room room = new Room();
        room.setType(RoomType.DM);
        room.setCreatedBy(user1);
        room.setName("dm_" + Math.min(user1.getId(), user2.getId()) + "_"
                + Math.max(user1.getId(), user2.getId()));

        Room savedRoom = roomRepository.save(room);

        RoomMember member1 = RoomMember.builder()
                .room(savedRoom)
                .user(user1)
                .roomRole(RoomRole.MEMBER)
                .joinedAt(LocalDateTime.now())
                .build();

        RoomMember member2 = RoomMember.builder()
                .room(savedRoom)
                .user(user2)
                .roomRole(RoomRole.MEMBER)
                .joinedAt(LocalDateTime.now())
                .build();

        List<RoomMember> members = roomMemberRepository.saveAll(List.of(member1, member2));
        savedRoom.setMembers(members);

        return roomMapper.toDto(savedRoom);
    }

    @Override
    @Transactional
    public RoomDto updateRoom(Long roomId, AddUsersToRoomDto addUsersToRoomDto) {
        // get request user
        User user = securityUtils.getRequestUser();

        List<Long> userIds = addUsersToRoomDto.getUserIds();

        // find the room
        Room room = roomRepository.findById(roomId).orElseThrow(
                () -> new ResourceNotFoundException("Room not found with id: " + roomId));

        // get existing DM members
        List<RoomMember> existingDmMembers = roomMemberRepository.findByRoom(room);

        if (room.getType().equals(RoomType.DM) && !userIds.isEmpty()) {

            // check if the users actually exist
            List<User> newUsers = userRepository.findAllById(userIds);

            // all the user ids from db
            List<Long> fetchedUserIds = newUsers.stream()
                    .map(User::getId)
                    .toList();

            // missing entries in db
            List<Long> missingUserIds = userIds.stream()
                    .filter(id -> !fetchedUserIds.contains(id))
                    .toList();

            // check if there are any users that do not exist in the db
            if (!missingUserIds.isEmpty()) {
                throw new ResourceNotFoundException("User with id '%s' not found".formatted(missingUserIds));
            }

            // merge existing DM member IDs + new user IDs, deduplicated
            List<Long> allUserIds = Stream.concat(
                    existingDmMembers.stream().map(rm -> rm.getUser().getId()),
                    userIds.stream()
            ).distinct().toList();

            // fetch all users at once
            List<User> allUsers = userRepository.findAllById(allUserIds);

            // use fetchedUserIds to create RoomMember entry and
            // create a new room with all the members including that of dm
            Room savedRoom = roomRepository.save(
                    Room.builder().name("Group from " + room.getName())
                            .type(RoomType.GROUP)
                            .createdBy(user)
                            .build()
            );

            log.info("New group room created from DM. id={}, name={}, createdBy={}",
                    savedRoom.getId(), savedRoom.getName(), user.getEmail());

            List<RoomMember> roomMembers = null;
//          create a new room member entry for each user
            List<RoomMember> allRoomMembers = allUsers.stream()
                    .map(u ->
                            RoomMember.builder()
                                    .room(savedRoom)
                                    .user(u)
                                    .roomRole(u.getId().equals(user.getId()) ? RoomRole.ADMIN : RoomRole.MEMBER)
                                    .joinedAt(LocalDateTime.now())
                                    .build())
                    .toList();
            roomMemberRepository.saveAll(allRoomMembers);

            savedRoom.setMembers(allRoomMembers);
            return roomMapper.toDto(savedRoom);
        } else {
            throw new BadRequestException("Room is not a DM or no users provided.");
        }

    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomListDto> getRooms() {
        User user = securityUtils.getRequestUser();
        List<RoomListDto> rooms = roomRepository.findRoomsForUser(user.getId());

        List<Long> roomIds = rooms.stream()
                .map(RoomListDto::getId)
                .toList();

        Map<Long, RecentMessageDto> recentMessagesByRoomId = roomRepository.getRecentMessagesForRooms(roomIds)
                .stream()
                .collect(Collectors.toMap(recentMessageDto -> recentMessageDto.getRoomId(), msg -> msg));

        rooms.forEach(
                room -> {
                    RecentMessageDto recentMessage = recentMessagesByRoomId.get(room.getId());
                    if (recentMessage != null
                            && (recentMessage.getContent() == null || recentMessage.getContent().isBlank())
                            && recentMessage.getMessageType() != null
                            && recentMessage.getMessageType() != MessageType.TEXT) {
                        recentMessage.setContent(recentMessage.getMessageType() == MessageType.IMAGE ? "Sent an image" : "Sent a video");
                    }
                    room.setRecentMessage(recentMessage);
                });

        return rooms;
    }

    @Override
    public List<RoomMessageDto> getAllMessagesForRoom(Long roomId) {
        // ill have to write a custom repo method for getting data in roommessagedto
        return roomRepository.getMessagesForRoom(roomId).stream()
                .peek(message -> {
                    if (message.getUserInfo() != null) {
                        message.getUserInfo().setProfileImageUrl(
                                s3Operations.getProfileImageAccessUrl(message.getUserInfo().getProfileImageUrl())
                        );
                    }
                    message.setMediaUrl(s3Operations.getMediaAccessUrl(message.getMediaUrl()));
                })
                .toList();
    }

    @Override
    @Transactional
    public void addUserToRoomGroup(AddUsersToRoomDto request, Long roomId) {
        List<Long> userIds = request.getUserIds();

        // check if the room exists
        Room room = roomRepository.findById(roomId).orElseThrow(
                () -> new ResourceNotFoundException("Room not found"));

        if (userIds.isEmpty()) {
            throw new BadRequestException("The list of users is empty.");
        }

        // check if the users actually exist
        List<User> usersList = userRepository.findAllById(userIds);

        // all the user ids from db
        List<Long> fetchedUserIds = usersList.stream()
                .map(User::getId)
                .toList();

        // missing entries in db
        List<Long> missingUserIds = userIds.stream()
                .filter(id -> !fetchedUserIds.contains(id))
                .toList();

        // check if there are any users that do not exist in the db
        if (!missingUserIds.isEmpty()) {
            throw new ResourceNotFoundException("User with id '%s' not found".formatted(missingUserIds));
        }

        List<RoomMember> newMembers = usersList.stream()
                .map(user -> RoomMember.builder()
                        .room(room)
                        .user(user)
                        .roomRole(RoomRole.MEMBER)
                        .joinedAt(LocalDateTime.now())
                        .build())
                .toList();
        List<RoomMember> roomMembers = roomMemberRepository.saveAll(newMembers);
        log.info("The room members {} are added in the room", roomMembers);
    }

    @Override
    @Transactional
    public void removeUserFromRoom(RemoveMembersDto request, Long roomId) {
        List<Long> deleteIds = request.getMembersId();
        // check if the users actually exist
        roomRepository.deleteAllById(deleteIds);

    }

    @Override
    @Transactional
    public void deleteRoom() {

    }
}
