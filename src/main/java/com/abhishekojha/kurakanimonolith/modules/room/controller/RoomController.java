package com.abhishekojha.kurakanimonolith.modules.room.controller;

import com.abhishekojha.kurakanimonolith.modules.room.dto.AddUsersToRoomDto;
import com.abhishekojha.kurakanimonolith.modules.room.dto.CreateRoomRequestDto;
import com.abhishekojha.kurakanimonolith.modules.room.dto.RemoveMembersDto;
import com.abhishekojha.kurakanimonolith.modules.room.dto.RoomDto;
import com.abhishekojha.kurakanimonolith.modules.room.service.RoomServiceImpl;
import com.abhishekojha.kurakanimonolith.modules.room_member.dto.RoomMemberDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.websocket.server.PathParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173/")
@Tag(name = "Rooms", description = "Room and membership management endpoints.")
public class RoomController {

    private final RoomServiceImpl roomService;

    @GetMapping("/room/{roomId}")
    public ResponseEntity<?> getAllRoomMembers(@PathVariable Long roomId) {
        List<RoomMemberDto> allMembers = roomService.getAllMembers(roomId);
        return new ResponseEntity<>(allMembers, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<?> createNewRoom(
            @Valid @RequestBody CreateRoomRequestDto createRoomRequestDto
    ) {
        RoomDto room = roomService.createRoom(createRoomRequestDto);
        return new ResponseEntity<>(room, HttpStatus.CREATED);
    }

    @PostMapping("/room/{room_id}/add")
    public ResponseEntity<?> addUsersToRoom(
            @Valid @RequestBody AddUsersToRoomDto addUsersToRoomDto,
            @PathVariable Long room_id
    ) {
        roomService.addUserToRoom(addUsersToRoomDto, room_id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/room/{room_id}/remove")
    public ResponseEntity<?> deleteUsersFromRoom(
            @Valid @RequestBody RemoveMembersDto removeMembersDto,
            @PathVariable Long room_id
    ) {
        roomService.removeUserFromRoom(removeMembersDto, room_id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
