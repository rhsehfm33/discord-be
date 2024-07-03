package team.discordbe.domain.invitation.friend.service;

import static team.discordbe.domain.friendship.constant.FriendStatus.*;

import java.util.List;

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
import team.discordbe.domain.friendship.model.Friendship;
import team.discordbe.domain.friendship.repository.FriendshipRepository;
import team.discordbe.domain.invitation.friend.dto.FriendInvitationResponseDto;
import team.discordbe.domain.invitation.friend.model.FriendInvitation;
import team.discordbe.domain.invitation.friend.repository.FriendInvitationRepository;
import team.discordbe.domain.user.model.User;
import team.discordbe.domain.user.repository.UserRepository;

@Service
@Transactional
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FriendInvitationService {
    private final FriendInvitationRepository friendInvitationRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    public void invite(Authentication authentication, String toUserNickName) {
        User fromUser = (User) authentication.getPrincipal();
        User toUser = userRepository.findByNickName(toUserNickName)
            .orElseThrow(() -> new IllegalArgumentException("Invalid nickname: " + toUserNickName));

        FriendInvitation friendInvitation = friendInvitationRepository
            .findByFromUserAndToUser(fromUser, toUser).orElse(null);
        if (friendInvitation != null) {
            throw new IllegalArgumentException("Invitation false");
        }
        friendInvitationRepository.save(new FriendInvitation(fromUser, toUser));
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

        MatchOperation matchOperation = Aggregation.match(Criteria.where("toUser.id").is(myUserId));
        LookupOperation lookupUser = Aggregation.lookup("users", "fromUser.$id", "_id", "toUserDetails");
        Aggregation aggregation = Aggregation.newAggregation(matchOperation, lookupUser);
        AggregationResults<FriendInvitation> results = mongoTemplate
            .aggregate(aggregation, "friend_invitations", FriendInvitation.class);

        List<FriendInvitationResponseDto> friendInvitationDtos = results.getMappedResults()
            .stream()
            .map(friendInvitation -> new FriendInvitationResponseDto(
                friendInvitation.getId(), friendInvitation.getFromUser()))
            .toList();

        return friendInvitationDtos;
    }

    public List<FriendInvitationResponseDto> getSentInvitations(Authentication authentication) {
        String myUserId = ((User) authentication.getPrincipal()).getId();

        MatchOperation matchOperation = Aggregation.match(Criteria.where("fromUser.id").is(myUserId));
        LookupOperation lookupUser = Aggregation.lookup("users", "toUser.$id", "_id", "toUserDetails");
        Aggregation aggregation = Aggregation.newAggregation(matchOperation, lookupUser);
        AggregationResults<FriendInvitation> results = mongoTemplate
            .aggregate(aggregation, "friend_invitations", FriendInvitation.class);

        List<FriendInvitationResponseDto> friendInvitationsDtos = results.getMappedResults().stream()
            .map(friendInvitation -> new FriendInvitationResponseDto(
                friendInvitation.getId(), friendInvitation.getToUser()
            ))
            .toList();

        return friendInvitationsDtos;
    }

    public void accept(Authentication authentication, String invitationId) {
        User fromUser = (User) authentication.getPrincipal();
        FriendInvitation friendInvitation = friendInvitationRepository.findById(invitationId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid invitationId: " + invitationId));
        User toUser = friendInvitation.getToUser();

        Friendship toUserFriendship = friendshipRepository.findByFromUserAndToUser(toUser, fromUser)
            .orElseThrow(() -> new IllegalArgumentException("Invalid toUser: " + toUser));
        Friendship fromUserFriendship = friendshipRepository.findByFromUserAndToUser(fromUser, toUser)
            .orElseThrow(() -> new IllegalArgumentException("Invalid fromUser: " + fromUser));
        toUserFriendship.setFriendStatus(FRIEND);
        fromUserFriendship.setFriendStatus(FRIEND);

        friendInvitationRepository.delete(friendInvitation);
    }
}
