package com.abhishekojha.kurakanimonolith.modules.friendRequest.dto;

import com.abhishekojha.kurakanimonolith.modules.friendRequest.model.RequestStatus;

import java.time.LocalDateTime;

public class FriendShipResponseDto {
    private Long id;
    private Long requesterId;
    private Long recipientId;
    private RequestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
