package com.abhishekojha.kurakanimonolith.modules.friendRequest.service;

import com.abhishekojha.kurakanimonolith.modules.friendRequest.dto.FriendShipResponseDto;

import java.util.List;

public interface FriendRequestService {
    void sendFriendRequest(Long recipientId);
    List<FriendShipResponseDto> listSentFriendRequest();
    List<FriendShipResponseDto> listIncomingFriendRequest();
    void acceptFriendRequest(Long requesterId);
    void denyFriendRequest(Long requesterId);
}
