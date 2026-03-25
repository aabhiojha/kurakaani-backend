package com.abhishekojha.kurakanimonolith.modules.friendRequest.dto;

import com.abhishekojha.kurakanimonolith.modules.friendRequest.model.RequestStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FriendShipRequestDto {
    private Long requesterId;
    private Long recipientId;
}
