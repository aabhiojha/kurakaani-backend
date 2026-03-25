package com.abhishekojha.kurakanimonolith.modules.friendRequest.service;

import com.abhishekojha.kurakanimonolith.modules.friendRequest.dto.FriendShipResponseDto;

import java.util.List;

public class FriendRequestServiceImpl implements FriendRequestService{
    @Override
    public void sendFriendRequest(Long recipientId) {
        //
    }

    @Override
    public List<FriendShipResponseDto> listSentFriendRequest() {
        return List.of();
    }

    @Override
    public List<FriendShipResponseDto> listIncomingFriendRequest() {
        return List.of();
    }

    @Override
    public void acceptFriendRequest(Long requesterId) {

    }

    @Override
    public void denyFriendRequest(Long requesterId) {

    }
}
