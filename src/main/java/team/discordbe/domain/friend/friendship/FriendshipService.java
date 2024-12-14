package team.discordbe.domain.friend.friendship;

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
import team.discordbe.infrastructure.friend.friendship.Friendship;
import team.discordbe.infrastructure.friend.friendship.FriendshipMongoRepository;
import team.discordbe.infrastructure.user.User;
import team.discordbe.infrastructure.user.UserMongoRepository;
import team.discordbe.interfaces.common.exception.CustomEntityNotFoundException;
import team.discordbe.interfaces.common.exception.CustomIllegalArgumentException;
import team.discordbe.interfaces.friend.friendship.FriendshipRequest;
import team.discordbe.interfaces.friend.friendship.FriendshipResponse;

@Service
@Transactional
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FriendshipService {
    private final UserMongoRepository userMongoRepository;
    private final FriendshipMongoRepository friendshipMongoRepository;
    private final MongoTemplate mongoTemplate;

    public List<FriendshipResponse> getFriendsByStatus(Authentication authentication, FriendStatus friendStatus) {
        String myUserId = ((User) authentication.getPrincipal()).getId();

        MatchOperation matchOperation = Aggregation.match(
            Criteria.where("fromUser.$id").is(new ObjectId(myUserId)).and("friendStatus").is(friendStatus)
        );
        LookupOperation lookupUser = Aggregation.lookup("users", "toUser.$id", "_id", "toUserDetails");
        Aggregation aggregation = Aggregation.newAggregation(matchOperation, lookupUser);
        AggregationResults<Friendship> friendships = mongoTemplate
            .aggregate(aggregation, "friendships", Friendship.class);

        List<FriendshipResponse> friendshipResponses = friendships.getMappedResults().stream()
            .map(friendship -> new FriendshipResponse(friendship.getId(), friendship.getToUser()))
            .toList();
        return friendshipResponses;
    }

    public FriendStatus getFriendshipStatus(Authentication authentication, String toUserNickName) throws
        CustomIllegalArgumentException {
        User fromUser = (User) authentication.getPrincipal();
        User toUser = userMongoRepository.findByNickName(toUserNickName).orElseThrow(() ->
            new CustomIllegalArgumentException(null, "Invalid nickname: " + toUserNickName));

        Optional<Friendship> friendship = friendshipMongoRepository.findByFromUserAndToUser(fromUser, toUser);
        return friendship.isEmpty() ? null : friendship.get().getFriendStatus();
    }

    public void updateFriendship(Authentication authentication, FriendshipRequest friendshipRequest)
        throws CustomIllegalArgumentException, CustomEntityNotFoundException {
        String toUserNickName = friendshipRequest.getToUserNickName();
        User fromUser = (User) authentication.getPrincipal();
        User toUser = userMongoRepository.findByNickName(toUserNickName)
            .orElseThrow(() -> new CustomEntityNotFoundException(null, "Invalid nickname: " + toUserNickName));

        Friendship friendship = friendshipMongoRepository.findByFromUserAndToUser(fromUser, toUser)
            .orElseThrow(() -> new CustomIllegalArgumentException(null, "Invalid friendship"));
        friendship.setFriendStatus(friendshipRequest.getFriendStatus());
        friendshipMongoRepository.save(friendship);
    }

    public void deleteFriendship(Authentication authentication, String friendshipId)
        throws CustomIllegalArgumentException {
        User fromUser = (User) authentication.getPrincipal();
        Friendship friendship = friendshipMongoRepository.findByIdAndFromUser(friendshipId, fromUser)
            .orElseThrow(() -> new CustomIllegalArgumentException(null, "Invalid friendship"));
        friendshipMongoRepository.delete(friendship);
    }
}
