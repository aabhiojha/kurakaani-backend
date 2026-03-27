package com.abhishekojha.kurakanimonolith.modules.room.controller;

import com.abhishekojha.kurakanimonolith.modules.room.dto.*;
import com.abhishekojha.kurakanimonolith.modules.room.dto.roomList.RoomListDto;
import com.abhishekojha.kurakanimonolith.modules.room.dto.roomMessage.RoomMessageDto;
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
@CrossOrigin({
        "http://localhost:5173/",
        "http://192.168.1.19:5173"
})
@Tag(name = "Rooms", description = "Room and membership management endpoints.")
public class RoomController {

    private final RoomServiceImpl roomService;

    @Operation(summary = "Get rooms for current user", description = "Returns all chat rooms the authenticated user is a member of.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of rooms returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping
    public ResponseEntity<List<?>> getAllRooms() {
        List<RoomListDto> rooms = roomService.getRooms();
        return new ResponseEntity<>(rooms, HttpStatus.OK);
    }

    @Operation(summary = "Get messages of room", description = "Returns all the messages of the room")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of messages returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("room/{roomId}/message")
    public ResponseEntity<List<?>> getMessagesForRoom(@PathVariable Long roomId) {
        List<RoomMessageDto> messages = roomService.getAllMessagesForRoom(roomId);
        return new ResponseEntity<>(messages, HttpStatus.OK);
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
    @PostMapping("/group")
    public ResponseEntity<?> createNewGroup(
            @Valid @RequestBody CreateRoomRequestDto createRoomRequestDto
    ) {
        RoomDto room = roomService.createRoomGroup(createRoomRequestDto);
        return new ResponseEntity<>(room, HttpStatus.CREATED);
    }

    // create a room with only two users i.e DM
    @Operation(summary = "Create a DM room", description = "Creates a DM. Must have one other user and both are members.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Room created, new room returned"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("/dm")
    public ResponseEntity<RoomDto> CreateNewDm(
            @RequestParam Long userId
    ) {
        RoomDto roomDm = roomService.createRoomDm(userId);
        return new ResponseEntity<>(roomDm, HttpStatus.CREATED);
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
