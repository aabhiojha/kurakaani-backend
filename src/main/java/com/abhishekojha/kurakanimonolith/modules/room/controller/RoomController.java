package com.abhishekojha.kurakanimonolith.modules.room.controller;

import com.abhishekojha.kurakanimonolith.modules.room.dto.AddUsersToRoomDto;
import com.abhishekojha.kurakanimonolith.modules.room.dto.CreateRoomRequestDto;
import com.abhishekojha.kurakanimonolith.modules.room.dto.RemoveMembersDto;
import com.abhishekojha.kurakanimonolith.modules.room.dto.RoomDto;
import com.abhishekojha.kurakanimonolith.modules.room.service.RoomServiceImpl;
import com.abhishekojha.kurakanimonolith.modules.room_member.dto.RoomMemberDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

    @Operation(summary = "Get rooms for current user", description = "Returns all chat rooms the authenticated user is a member of.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of rooms returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping
    public ResponseEntity<List<RoomDto>> getAllRooms(){
        List<RoomDto> rooms = roomService.getRooms();
        return new ResponseEntity<>(rooms, HttpStatus.OK);
    }

    @Operation(summary = "Get room members", description = "Returns all members of the specified room. The caller must be a member of the room.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of room members returned"),
            @ApiResponse(responseCode = "403", description = "Caller is not a member of this room"),
            @ApiResponse(responseCode = "404", description = "Room not found")
    })
    @GetMapping("/room/{roomId}")
    public ResponseEntity<?> getAllRoomMembers(@PathVariable Long roomId) {
        List<RoomMemberDto> allMembers = roomService.getAllMembers(roomId);
        return new ResponseEntity<>(allMembers, HttpStatus.OK);
    }

    @Operation(summary = "Create a room", description = "Creates a new chat room. The authenticated user is automatically added as the owner/admin of the room.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Room created, new room returned"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping
    public ResponseEntity<?> createNewRoom(
            @Valid @RequestBody CreateRoomRequestDto createRoomRequestDto
    ) {
        RoomDto room = roomService.createRoom(createRoomRequestDto);
        return new ResponseEntity<>(room, HttpStatus.CREATED);
    }

    @Operation(summary = "Add users to a room", description = "Adds one or more users to an existing room by their IDs. The caller must be a member of the room.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "403", description = "Caller is not a member of this room"),
            @ApiResponse(responseCode = "404", description = "Room or user not found")
    })
    @PostMapping("/room/{room_id}/add")
    public ResponseEntity<?> addUsersToRoom(
            @Valid @RequestBody AddUsersToRoomDto addUsersToRoomDto,
            @PathVariable Long room_id
    ) {
        roomService.addUserToRoom(addUsersToRoomDto, room_id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "Remove users from a room", description = "Removes one or more members from a room by their IDs. The caller must be a member of the room.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Users removed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "403", description = "Caller is not a member of this room"),
            @ApiResponse(responseCode = "404", description = "Room or user not found")
    })
    @PostMapping("/room/{room_id}/remove")
    public ResponseEntity<?> deleteUsersFromRoom(
            @Valid @RequestBody RemoveMembersDto removeMembersDto,
            @PathVariable Long room_id
    ) {
        roomService.removeUserFromRoom(removeMembersDto, room_id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
