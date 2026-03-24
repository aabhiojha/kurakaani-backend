package com.abhishekojha.kurakanimonolith.modules.room.service;

import com.abhishekojha.kurakanimonolith.modules.room.dto.*;
import com.abhishekojha.kurakanimonolith.modules.room.dto.roomList.RoomListDto;
import com.abhishekojha.kurakanimonolith.modules.room.dto.roomMessage.RoomMessageDto;
import com.abhishekojha.kurakanimonolith.modules.room_member.dto.RoomMemberDto;

import java.util.List;

public interface RoomService {
    List<RoomMemberDto> getAllMembers(Long roomId);

    RoomDto createRoom(CreateRoomRequestDto createRoomRequestDto);

    List<RoomListDto> getRooms();

    List<RoomMessageDto> getAllMessagesForRoom(Long roomId);

    void addUserToRoom(AddUsersToRoomDto addUsersToRoomDto,Long room_id);

    void removeUserFromRoom(RemoveMembersDto removeMembersDto, Long room_id);

    void deleteRoom();
}
