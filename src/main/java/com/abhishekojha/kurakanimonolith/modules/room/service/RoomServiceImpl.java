package com.abhishekojha.kurakanimonolith.modules.room.service;

import com.abhishekojha.kurakanimonolith.common.exception.exceptions.BadRequestException;
import com.abhishekojha.kurakanimonolith.common.exception.exceptions.DuplicateResourceException;
import com.abhishekojha.kurakanimonolith.common.exception.exceptions.ResourceNotFoundException;
import com.abhishekojha.kurakanimonolith.common.exception.exceptions.UnauthorizedException;
import com.abhishekojha.kurakanimonolith.common.security.SecurityUtils;
import com.abhishekojha.kurakanimonolith.modules.room.dto.*;
import com.abhishekojha.kurakanimonolith.modules.room.dto.roomList.RecentMessageDto;
import com.abhishekojha.kurakanimonolith.modules.room.dto.roomList.RoomListDto;
import com.abhishekojha.kurakanimonolith.modules.room.dto.roomMessage.RoomMessageDto;
import com.abhishekojha.kurakanimonolith.modules.room.mapper.RoomMapper;
import com.abhishekojha.kurakanimonolith.modules.room.model.Room;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public RoomDto createRoom(CreateRoomRequestDto createRoomRequestDto) {
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
                room -> room.setRecentMessage(
                        recentMessagesByRoomId.get(room.getId())));

        return rooms;
    }

    @Override
    public List<RoomMessageDto> getAllMessagesForRoom(Long roomId) {
        // ill have to write a custom repo method for getting data in roommessagedto
        return roomRepository.getMessagesForRoom(roomId);
    }

    @Override
    @Transactional
    public void addUserToRoom(AddUsersToRoomDto request, Long roomId) {
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
        userRepository.deleteAllById(deleteIds);

    }

    @Override
    @Transactional
    public void deleteRoom() {

    }
}
