package discord.chat.endpoint.domain.friend.invitation;

import static discord.chat.common.infrastructure.friend.friendship.FriendStatus.*;

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
import discord.chat.common.infrastructure.friend.friendship.Friendship;
import discord.chat.common.infrastructure.friend.friendship.FriendshipMongoRepository;
import discord.chat.common.infrastructure.friend.invitation.FriendInvitation;
import discord.chat.common.infrastructure.friend.invitation.FriendInvitationMongoRepository;
import discord.chat.common.infrastructure.user.User;
import discord.chat.common.infrastructure.user.UserMongoRepository;
import discord.chat.common.exception.CustomEntityNotFoundException;
import discord.chat.endpoint.interfaces.friend.invitation.FriendInvitationResponse;

@Service
@Transactional
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FriendInvitationService {
    private final FriendInvitationMongoRepository friendInvitationMongoRepository;
    private final FriendshipMongoRepository friendshipMongoRepository;
    private final UserMongoRepository userMongoRepository;
    private final MongoTemplate mongoTemplate;

    public void invite(Authentication authentication, String toUserNickName)
        throws CustomEntityNotFoundException {
        User fromUser = (User) authentication.getPrincipal();
        User toUser = userMongoRepository.findByNickName(toUserNickName)
            .orElseThrow(() -> new CustomEntityNotFoundException(null, "Invalid nickname: " + toUserNickName));

        Optional<FriendInvitation> friendInvitation = friendInvitationMongoRepository
            .findByFromUserAndToUser(fromUser, toUser);
        if (friendInvitation.isEmpty()) {
            friendInvitationMongoRepository.save(new FriendInvitation(fromUser, toUser));
        }
    }

    public List<FriendInvitationResponse> getFriendInvitations(Authentication authentication, boolean isPassive) {
        if (isPassive) {
            return getReceivedInvitations(authentication);
        } else {
            return getSentInvitations(authentication);
        }
    }

    public List<FriendInvitationResponse> getReceivedInvitations(Authentication authentication) {
        String myUserId = ((User) authentication.getPrincipal()).getId();

        MatchOperation matchOperation = Aggregation.match(Criteria.where("toUser.$id").is(new ObjectId(myUserId)));
        LookupOperation lookupUser = Aggregation.lookup("users", "fromUser.$id", "_id", "toUserDetails");
        Aggregation aggregation = Aggregation.newAggregation(matchOperation, lookupUser);
        AggregationResults<FriendInvitation> results = mongoTemplate
            .aggregate(aggregation, "friend_invitations", FriendInvitation.class);

        List<FriendInvitationResponse> friendInvitationDtos = results.getMappedResults().stream()
            .map(friendInvitation -> new FriendInvitationResponse(
                friendInvitation.getId(), friendInvitation.getFromUser()))
            .toList();

        return friendInvitationDtos;
    }

    public List<FriendInvitationResponse> getSentInvitations(Authentication authentication) {
        String myUserId = ((User) authentication.getPrincipal()).getId();

        MatchOperation matchOperation = Aggregation.match(Criteria.where("fromUser.$id").is(new ObjectId(myUserId)));
        LookupOperation lookupUser = Aggregation.lookup("users", "toUser.$id", "_id", "toUserDetails");
        Aggregation aggregation = Aggregation.newAggregation(matchOperation, lookupUser);
        AggregationResults<FriendInvitation> results = mongoTemplate
            .aggregate(aggregation, "friend_invitations", FriendInvitation.class);

        List<FriendInvitationResponse> friendInvitationsDtos = results.getMappedResults().stream()
            .map(friendInvitation -> new FriendInvitationResponse(
                friendInvitation.getId(), friendInvitation.getToUser()))
            .toList();

        return friendInvitationsDtos;
    }

    public void accept(Authentication authentication, String invitationId)
        throws CustomEntityNotFoundException {
        User invitee = (User) authentication.getPrincipal();
        FriendInvitation friendInvitation = friendInvitationMongoRepository.findByIdAndToUser(invitationId, invitee)
            .orElseThrow(() -> new CustomEntityNotFoundException(null, "Invitation not found"));
        User inviter = friendInvitation.getFromUser();

        Friendship inviteeFriendship = friendshipMongoRepository.findByFromUserAndToUser(inviter, invitee)
            .orElse(new Friendship(inviter, invitee, FRIEND));
        Friendship invitorFriendship = friendshipMongoRepository.findByFromUserAndToUser(invitee, inviter)
            .orElse(new Friendship(invitee, inviter, FRIEND));
        inviteeFriendship.setFriendStatus(FRIEND);
        invitorFriendship.setFriendStatus(FRIEND);
        friendshipMongoRepository.save(inviteeFriendship);
        friendshipMongoRepository.save(invitorFriendship);

        friendInvitationMongoRepository.delete(friendInvitation);
    }

    public void cancel(Authentication authentication, String invitationId)
        throws CustomEntityNotFoundException {
        User inviter = (User) authentication.getPrincipal();
        FriendInvitation friendInvitation = friendInvitationMongoRepository.findByIdAndFromUser(invitationId, inviter)
            .orElseThrow(() -> new CustomEntityNotFoundException(null, "Invitation not found"));
        friendInvitationMongoRepository.delete(friendInvitation);
    }
}
