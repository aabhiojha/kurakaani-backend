package com.abhishekojha.kurakanimonolith.modules.friendRequest.dto;

import com.abhishekojha.kurakanimonolith.modules.friendRequest.model.enums.FriendRequestStatus;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FriendshipDecisionResponse {
    private Long requesterId;
    private Long recipientId;
    private FriendRequestStatus status;
}
