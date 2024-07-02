package team.discordbe.domain.friendship.service;

import static team.discordbe.domain.friendship.constant.FriendStatus.*;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.discordbe.domain.friendship.constant.FriendStatus;
import team.discordbe.domain.friendship.model.Friendship;
import team.discordbe.domain.friendship.repository.FriendshipRepository;
import team.discordbe.domain.user.dto.UserResponseDto;
import team.discordbe.domain.user.model.User;
import team.discordbe.domain.user.repository.UserRepository;

@Service
@Transactional
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FriendshipService {
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;

    public List<UserResponseDto> getFriendsByStatus(Authentication authentication, FriendStatus friendStatus) {
        String fromUserId = ((User) authentication.getPrincipal()).getId();
        List<Friendship> friendships = friendshipRepository.findByFromUserIdAndFriendStatus(fromUserId, friendStatus);
        List<String> targetUserIds = friendships.stream().map(Friendship::getFromUserId).toList();
        List<User> targetUsers = userRepository.findAllById(targetUserIds);
        return targetUsers.stream().map(UserResponseDto::new).toList();
    }

    public FriendStatus getFriendshipStatus(Authentication authentication, String toUserNickName) {
        String fromUserId = ((User) authentication.getPrincipal()).getId();
        User toUser = userRepository.findByNickName(toUserNickName).orElseThrow(() ->
            new IllegalArgumentException("Invalid nickname: " + toUserNickName));
        String toUserId = toUser.getId();

        Friendship friendship = friendshipRepository.findByFromUserIdAndToUserId(fromUserId, toUserId).orElse(null);
        if (friendship == null) {
            return null;
        } else {
            return friendship.getFriendStatus();
        }
    }

    public List<UserResponseDto> getReceivedInvitations(Authentication authentication) {
        String fromUserId = ((User) authentication.getPrincipal()).getId();
        List<Friendship> friendships = friendshipRepository.findByToUserIdAndFriendStatus(fromUserId, INVITING);
        List<String> targetUserIds = friendships.stream().map(Friendship::getFromUserId).toList();
        List<User> targetUsers = userRepository.findAllById(targetUserIds);
        return targetUsers.stream().map(UserResponseDto::new).toList();
    }

    public void acceptFriendInvitation(Authentication authentication, String inviterNickName) {
        String inviteeId = ((User) authentication.getPrincipal()).getId();
        String inviterId = userRepository.findByNickName(inviterNickName).orElseThrow(() ->
            new IllegalArgumentException("Invalid nickname: " + inviterNickName)).getId();

        Friendship inviterFriendship = friendshipRepository.findByFromUserIdAndToUserId(inviterId, inviteeId).orElse(null);
        Friendship inviteeFriendship = friendshipRepository.findByFromUserIdAndToUserId(inviteeId, inviterId).orElse(null);
        if (inviterFriendship != null && inviteeFriendship == null &&
            inviterFriendship.getFriendStatus().equals(INVITING)) {
            inviterFriendship.setFriendStatus(FRIEND);
            friendshipRepository.save(new Friendship(inviteeId, inviterId, FRIEND));
        } else {
            throw new IllegalArgumentException("Invalid acceptance");
        }
    }

    public void sendFriendInvitation(Authentication authentication, String toUserNickName) throws IllegalArgumentException {
        String fromUserId = ((User) authentication.getPrincipal()).getId();
        String toUserId = userRepository.findByNickName(toUserNickName).orElseThrow(() ->
            new IllegalArgumentException("Invalid nickname: " + toUserNickName)).getId();

        Friendship friendship = friendshipRepository.findByFromUserIdAndToUserId(fromUserId, toUserId).orElse(null);
        if (friendship == null) {
            friendshipRepository.save(new Friendship(fromUserId, toUserId, FRIEND));
        } else {
            throw new IllegalArgumentException("It already has " + friendship.getFriendStatus() + " status.");
        }
    }

    public void blockUser(Authentication authentication, String toUserNickName) throws IllegalArgumentException {
        String fromUserId = ((User) authentication.getPrincipal()).getId();
        String toUserId = userRepository.findByNickName(toUserNickName).orElseThrow(() ->
            new IllegalArgumentException("Invalid nickname: " + toUserNickName)).getId();

        Friendship friendship = friendshipRepository.findByFromUserIdAndToUserId(fromUserId, toUserId).orElse(null);
        if (friendship == null) {
            friendshipRepository.save(new Friendship(fromUserId, toUserId, BLOCKING));
        } else {
            friendship.setFriendStatus(BLOCKING);
        }
    }

    public void deleteFriend(Authentication authentication, String toUserNickName) throws IllegalArgumentException {
        String fromUserId = ((User) authentication.getPrincipal()).getId();
        String toUserId = userRepository.findByNickName(toUserNickName).orElseThrow(() ->
            new IllegalArgumentException("Invalid nickname: " + toUserNickName)).getId();

        friendshipRepository.findByFromUserIdAndToUserId(fromUserId, toUserId).ifPresent(friendshipRepository::delete);
    }
}
