package com.abhishekojha.kurakanimonolith.modules.room_member.dto;

import com.abhishekojha.kurakanimonolith.modules.room.model.Room;
import com.abhishekojha.kurakanimonolith.modules.room_member.model.RoomRole;
import com.abhishekojha.kurakanimonolith.modules.user.AppUser;

import java.time.LocalDateTime;

public class RoomMemberDto {
    private Long roomMemberId;
    private Long roomId;
    private Long userId;
    private RoomRole roomRole;
    private LocalDateTime joinedAt;

}
