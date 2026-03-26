package com.abhishekojha.kurakanimonolith.modules.friendRequest.service;

import com.abhishekojha.kurakanimonolith.common.exception.exceptions.BadRequestException;
import com.abhishekojha.kurakanimonolith.common.exception.exceptions.DuplicateResourceException;
import com.abhishekojha.kurakanimonolith.common.exception.exceptions.ResourceNotFoundException;
import com.abhishekojha.kurakanimonolith.common.security.SecurityUtils;
import com.abhishekojha.kurakanimonolith.modules.friendRequest.dto.FriendShipDto;
import com.abhishekojha.kurakanimonolith.modules.friendRequest.mapper.FriendShipMapper;
import com.abhishekojha.kurakanimonolith.modules.friendRequest.model.Friendship;
import com.abhishekojha.kurakanimonolith.modules.friendRequest.model.enums.FriendRequestResponse;
import com.abhishekojha.kurakanimonolith.modules.friendRequest.model.enums.FriendRequestStatus;
import com.abhishekojha.kurakanimonolith.modules.friendRequest.repository.FriendShipRepository;
import com.abhishekojha.kurakanimonolith.modules.user.model.User;
import com.abhishekojha.kurakanimonolith.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendRequestServiceImpl implements FriendRequestService {

    private final FriendShipRepository friendShipRepository;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final FriendShipMapper friendShipMapper;

    @Override
    public void sendFriendRequest(Long userId) {

        User user = securityUtils.getRequestUser();

        User recipient = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Friendship friendship = Friendship.builder().requester(user).status(FriendRequestStatus.PENDING).recipient(recipient).build();

        // TODO: cannot send request to themself
        if (userId.equals(user.getId())){
            throw new BadRequestException("User cannot send friend request to themselves");
        }

        // save to db
        Friendship save = friendShipRepository.save(friendship);
        log.info("The friendship object is saved in db");

        // we gotta send this as notification to the recipient user.
        notificationService.sendFriendRequestNotification(recipient.getUsername(), friendShipMapper.toDto(save));
        log.info("The friend request notification is sent to the recipient");
    }

    @Override
    public void respondToFriendRequest(Long userId, FriendRequestResponse response) {

        // the one responding to the request
        User responder = securityUtils.getRequestUser();
        log.debug("Request responding user: {}", responder.getId());

        // the one who sent the request
        User requester = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        log.debug("Request sending user: {}", requester.getId());

        // get the friendship object the user need to respond to
        Friendship friendship = friendShipRepository.findByRequesterAndRecipient(requester, responder);
        log.info("The friendship object is retrieved");

        friendship.setStatus(response == FriendRequestResponse.ACCEPT ?
                FriendRequestStatus.ACCEPTED :
                FriendRequestStatus.REJECTED);

        Friendship save = friendShipRepository.save(friendship);
        log.info("The friendship request status is set successfully");

        // we gotta send this as notification to the recipient user.
        // conditional notification sending
        if (response == FriendRequestResponse.ACCEPT){
            notificationService.sendFriendRequestAccepted(requester.getUsername(), friendShipMapper.toDto(save));
        } else {
            notificationService.sendFriendRequestRejected(requester.getUsername(), friendShipMapper.toDto(save));
        }
        log.info("The request status notification is sent to the requester");
    }

    @Override
    public void cancelFriendRequest(Long userId) {
        User user = securityUtils.getRequestUser();

        // the one who sent the request
        User recipient = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // get the friendship object the user need to respond to
        Friendship friendship = friendShipRepository.findByRequesterAndRecipient(user, recipient);
        log.info("The friendship object is found");

        // for cancel request we delete the friendship request
        friendShipRepository.delete(friendship);
        log.info("The friendship object is deleted successfully");
    }

    @Override
    public void unfriend(Long userId) {
        User user = securityUtils.getRequestUser();

        // the one who sent the request
        User recipient = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // get the friendship object the user need to respond to
        Friendship friendship = friendShipRepository.findByRequesterAndRecipient(user, recipient);

        // delete the friendship request by itself
        // for cancel request we delete the friendship request
        friendShipRepository.delete(friendship);
        log.info("The friendship object is deleted successfully");
    }

    @Override
    public List<FriendShipDto> getSentRequests() {

        // return all the request pending objects
        User user = securityUtils.getRequestUser();

        List<Friendship> friendships = friendShipRepository.findByRequester(user);

        List<Friendship> sentRequests = friendships.stream()
                .filter(friendship -> friendship.getStatus() == FriendRequestStatus.PENDING)
                .toList();

        log.info("The pending requests are retrieved");
        return friendShipMapper.toListDto(sentRequests);
    }

    @Override
    public List<FriendShipDto> getIncomingRequests() {
        // return all the received pending requests
        User user = securityUtils.getRequestUser();

        List<Friendship> friendshipRequests = friendShipRepository.findByRecipient(user);

        List<Friendship> incomingRequests = friendshipRequests.stream()
                .filter(friendship -> friendship.getStatus() == FriendRequestStatus.PENDING)
                .toList();

        log.info("The incoming requests are retrieved");
        return friendShipMapper.toListDto(incomingRequests);
    }

    @Override
    public List<FriendShipDto> getFriends() {
        // return all the received pending requests
        User user = securityUtils.getRequestUser();

        List<Friendship> allFriendshipObjs = friendShipRepository.findByRequesterOrRecipientAndStatus(user, user, FriendRequestStatus.ACCEPTED);

        List<Friendship> friendsList = allFriendshipObjs.stream()
                .filter(friendship -> friendship.getStatus() == FriendRequestStatus.ACCEPTED)
                .toList();

        log.info("The friends are retrieved");
        return friendShipMapper.toListDto(friendsList);
    }

    @Override
    public List<User> getAllUsers() {
//        List<User> all = userRepository.findAll();
        ;
            return List.of();
    }
}
