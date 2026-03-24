package com.abhishekojha.kurakanimonolith.modules.room.dto.roomMessage;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class RoomMessageDto {
    private Long id;
    private Long roomId;
    private UserinfoDto userInfo;
    private String content;
    private Boolean isEdited;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RoomMessageDto(Long id, Long roomId, String content, Boolean isEdited, Boolean isDeleted, LocalDateTime createdAt, LocalDateTime updatedAt, Long userId, String username, String profileImageUrl) {
        this.id = id;
        this.roomId = roomId;
        this.content = content;
        this.isEdited = isEdited;
        this.isDeleted = isDeleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.userInfo = new UserinfoDto(userId, username, profileImageUrl);
    }
}
