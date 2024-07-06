package team.discordbe.domain.friend.invitation.service;

import static team.discordbe.domain.friend.friendship.constant.FriendStatus.*;

import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.discordbe.domain.friend.friendship.model.Friendship;
import team.discordbe.domain.friend.friendship.repository.FriendshipRepository;
import team.discordbe.domain.friend.invitation.dto.FriendInvitationResponseDto;
import team.discordbe.domain.friend.invitation.model.FriendInvitation;
import team.discordbe.domain.friend.invitation.repository.FriendInvitationRepository;
import team.discordbe.domain.user.model.User;
import team.discordbe.domain.user.repository.UserRepository;
import team.discordbe.global.exception.CustomEntityNotFoundException;

@Service
@Transactional
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FriendInvitationService {
    private final FriendInvitationRepository friendInvitationRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    public void invite(Authentication authentication, String toUserNickName)
        throws CustomEntityNotFoundException {
        User fromUser = (User) authentication.getPrincipal();
        User toUser = userRepository.findByNickName(toUserNickName)
            .orElseThrow(() -> new CustomEntityNotFoundException(null, "Invalid nickname: " + toUserNickName));

        Optional<FriendInvitation> friendInvitation = friendInvitationRepository
            .findByFromUserAndToUser(fromUser, toUser);
        if (friendInvitation.isEmpty()) {
            friendInvitationRepository.save(new FriendInvitation(fromUser, toUser));
        }
    }

    public List<FriendInvitationResponseDto> getFriendInvitations(Authentication authentication, boolean isPassive) {
        if (isPassive) {
            return getReceivedInvitations(authentication);
        } else {
            return getSentInvitations(authentication);
        }
    }

    public List<FriendInvitationResponseDto> getReceivedInvitations(Authentication authentication) {
        String myUserId = ((User) authentication.getPrincipal()).getId();

        MatchOperation matchOperation = Aggregation.match(Criteria.where("toUser.$id").is(new ObjectId(myUserId)));
        LookupOperation lookupUser = Aggregation.lookup("users", "fromUser.$id", "_id", "toUserDetails");
        Aggregation aggregation = Aggregation.newAggregation(matchOperation, lookupUser);
        AggregationResults<FriendInvitation> results = mongoTemplate
            .aggregate(aggregation, "friend_invitations", FriendInvitation.class);

        List<FriendInvitationResponseDto> friendInvitationDtos = results.getMappedResults().stream()
            .map(friendInvitation -> new FriendInvitationResponseDto(
                friendInvitation.getId(), friendInvitation.getFromUser()))
            .toList();

        return friendInvitationDtos;
    }

    public List<FriendInvitationResponseDto> getSentInvitations(Authentication authentication) {
        String myUserId = ((User) authentication.getPrincipal()).getId();

        MatchOperation matchOperation = Aggregation.match(Criteria.where("fromUser.$id").is(new ObjectId(myUserId)));
        LookupOperation lookupUser = Aggregation.lookup("users", "toUser.$id", "_id", "toUserDetails");
        Aggregation aggregation = Aggregation.newAggregation(matchOperation, lookupUser);
        AggregationResults<FriendInvitation> results = mongoTemplate
            .aggregate(aggregation, "friend_invitations", FriendInvitation.class);

        List<FriendInvitationResponseDto> friendInvitationsDtos = results.getMappedResults().stream()
            .map(friendInvitation -> new FriendInvitationResponseDto(
                friendInvitation.getId(), friendInvitation.getToUser()))
            .toList();

        return friendInvitationsDtos;
    }

    public void accept(Authentication authentication, String invitationId)
        throws CustomEntityNotFoundException {
        User invitee = (User) authentication.getPrincipal();
        FriendInvitation friendInvitation = friendInvitationRepository.findByIdAndToUser(invitationId, invitee)
            .orElseThrow(() -> new CustomEntityNotFoundException(null, "Invitation not found"));
        User inviter = friendInvitation.getFromUser();

        Friendship inviteeFriendship = friendshipRepository.findByFromUserAndToUser(inviter, invitee)
            .orElse(new Friendship(inviter, invitee, FRIEND));
        Friendship invitorFriendship = friendshipRepository.findByFromUserAndToUser(invitee, inviter)
            .orElse(new Friendship(invitee, inviter, FRIEND));
        inviteeFriendship.setFriendStatus(FRIEND);
        invitorFriendship.setFriendStatus(FRIEND);
        friendshipRepository.save(inviteeFriendship);
        friendshipRepository.save(invitorFriendship);

        friendInvitationRepository.delete(friendInvitation);
    }

    public void cancel(Authentication authentication, String invitationId)
        throws CustomEntityNotFoundException {
        User inviter = (User) authentication.getPrincipal();
        FriendInvitation friendInvitation = friendInvitationRepository.findByIdAndToUser(invitationId, inviter)
            .orElseThrow(() -> new CustomEntityNotFoundException(null, "Invitation not found"));
        friendInvitationRepository.delete(friendInvitation);
    }
}
