package com.abhishekojha.kurakanimonolith.modules.room.service;

import com.abhishekojha.kurakanimonolith.modules.room.dto.AddUsersToRoomDto;
import com.abhishekojha.kurakanimonolith.modules.room.dto.CreateRoomRequestDto;
import com.abhishekojha.kurakanimonolith.modules.room.dto.RemoveMembersDto;
import com.abhishekojha.kurakanimonolith.modules.room.dto.RoomDto;
import com.abhishekojha.kurakanimonolith.modules.room_member.dto.RoomMemberDto;
import com.abhishekojha.kurakanimonolith.modules.room_member.model.RoomMember;
import org.springframework.boot.json.JsonWriter;

import java.util.List;

public interface RoomService {
    List<RoomMemberDto> getAllMembers(Long roomId);

    RoomDto createRoom(CreateRoomRequestDto createRoomRequestDto);

    List<RoomDto> getRooms();

    void addUserToRoom(AddUsersToRoomDto addUsersToRoomDto,Long room_id);

    void removeUserFromRoom(RemoveMembersDto removeMembersDto, Long room_id);

    void deleteRoom();
}
