package com.abhishekojha.kurakanimonolith.modules.room.service;

import com.abhishekojha.kurakanimonolith.modules.room.dto.AddUsersToRoomDto;
import com.abhishekojha.kurakanimonolith.modules.room.dto.CreateRoomRequestDto;
import com.abhishekojha.kurakanimonolith.modules.room.dto.RoomDto;

import java.util.List;

public interface RoomService {
    RoomDto createRoom(CreateRoomRequestDto createRoomRequestDto);

    List<RoomDto> getRooms();

    void addUserToRoom(AddUsersToRoomDto addUsersToRoomDto,Long room_id);

    void removeUserFromRoom();

    void deleteRoom();
}
