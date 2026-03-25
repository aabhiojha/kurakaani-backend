package com.abhishekojha.kurakanimonolith.modules.friendRequest.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FriendShipRequestDto {
    private Long requesterId;
    private Long recipientId;
}
