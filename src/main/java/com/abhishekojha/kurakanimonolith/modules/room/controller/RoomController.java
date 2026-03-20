package com.abhishekojha.kurakanimonolith.modules.room.controller;

import com.abhishekojha.kurakanimonolith.modules.room.dto.CreateRoomRequestDto;
import com.abhishekojha.kurakanimonolith.modules.room.dto.RoomDto;
import com.abhishekojha.kurakanimonolith.modules.room.service.RoomService;
import com.abhishekojha.kurakanimonolith.modules.room.service.RoomServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public RoomDto createNewRoom(@RequestBody CreateRoomRequestDto createRoomRequestDto){
        return roomService.createRoom(createRoomRequestDto);
    }
}
