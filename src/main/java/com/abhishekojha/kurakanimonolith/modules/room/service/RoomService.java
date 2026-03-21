package com.abhishekojha.kurakanimonolith.modules.room.service;

import com.abhishekojha.kurakanimonolith.modules.room.dto.CreateRoomRequestDto;
import com.abhishekojha.kurakanimonolith.modules.room.dto.RoomDto;

import java.util.List;

public interface RoomService {
    RoomDto createRoom(CreateRoomRequestDto createRoomRequestDto);

    List<RoomDto> getRooms();
}
