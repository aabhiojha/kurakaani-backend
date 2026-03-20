package com.abhishekojha.kurakanimonolith.modules.room.dto;

import com.abhishekojha.kurakanimonolith.modules.message.model.Message;
import com.abhishekojha.kurakanimonolith.modules.room.model.RoomType;
import com.abhishekojha.kurakanimonolith.modules.room_member.model.RoomMember;
import com.abhishekojha.kurakanimonolith.modules.user.AppUser;

import java.time.LocalDateTime;
import java.util.List;

public class RoomDto {

    private Long id;
    private String name;
    private String description;
    private List<RoomMember> members;
    private List<Message> messages;
    private RoomType type;
    private AppUser created_by;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;

}
