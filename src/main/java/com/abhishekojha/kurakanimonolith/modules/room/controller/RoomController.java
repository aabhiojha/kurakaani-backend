package com.abhishekojha.kurakanimonolith.modules.room.controller;

import com.abhishekojha.kurakanimonolith.modules.room.dto.AddUsersToRoomDto;
import com.abhishekojha.kurakanimonolith.modules.room.dto.CreateRoomRequestDto;
import com.abhishekojha.kurakanimonolith.modules.room.dto.RoomDto;
import com.abhishekojha.kurakanimonolith.modules.room.service.RoomService;
import com.abhishekojha.kurakanimonolith.modules.room.service.RoomServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomServiceImpl roomService;

    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public RoomDto createNewRoom(@Valid @RequestBody CreateRoomRequestDto createRoomRequestDto){
        return roomService.createRoom(createRoomRequestDto);
    }

    @PostMapping("/room/{room_id}")
    public ResponseEntity<?> addUsersToRoom(
            @Valid @RequestBody AddUsersToRoomDto addUsersToRoomDto,
            @PathVariable Long room_id
    ){
         roomService.addUserToRoom(addUsersToRoomDto, room_id);
         return new ResponseEntity<>(HttpStatus.OK);
    }
}
