package team.discordbe.domain.friendship.service;

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
import team.discordbe.domain.friendship.constant.FriendStatus;
import team.discordbe.domain.friendship.dto.FriendshipRequestDto;
import team.discordbe.domain.friendship.dto.FriendshipResponseDto;
import team.discordbe.domain.friendship.model.Friendship;
import team.discordbe.domain.friendship.repository.FriendshipRepository;
import team.discordbe.domain.user.model.User;
import team.discordbe.domain.user.repository.UserRepository;
import team.discordbe.global.exception.CustomEntityNotFoundException;
import team.discordbe.global.exception.CustomIllegalArgumentException;

@Service
@Transactional
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FriendshipService {
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final MongoTemplate mongoTemplate;

    public List<FriendshipResponseDto> getFriendsByStatus(Authentication authentication, FriendStatus friendStatus) {
        String myUserId = ((User) authentication.getPrincipal()).getId();

        MatchOperation matchOperation = Aggregation.match(
            Criteria.where("fromUser.$id").is(new ObjectId(myUserId)).and("friendStatus").is(friendStatus)
        );
        LookupOperation lookupUser = Aggregation.lookup("users", "toUser.$id", "_id", "toUserDetails");
        Aggregation aggregation = Aggregation.newAggregation(matchOperation, lookupUser);
        AggregationResults<Friendship> friendships = mongoTemplate
            .aggregate(aggregation, "friendships", Friendship.class);

        List<FriendshipResponseDto> friendshipResponseDtos = friendships.getMappedResults()
            .stream()
            .map(friendship -> new FriendshipResponseDto(friendship.getId(), friendship.getToUser()))
            .toList();
        return friendshipResponseDtos;
    }

    public FriendStatus getFriendshipStatus(Authentication authentication, String toUserNickName) throws
        CustomIllegalArgumentException {
        User fromUser = (User) authentication.getPrincipal();
        User toUser = userRepository.findByNickName(toUserNickName).orElseThrow(() ->
            new CustomIllegalArgumentException(null, "Invalid nickname: " + toUserNickName));

        Optional<Friendship> friendship = friendshipRepository.findByFromUserAndToUser(fromUser, toUser);
        return friendship.isEmpty() ? null : friendship.get().getFriendStatus();
    }

    public void updateFriendship(Authentication authentication, FriendshipRequestDto friendshipRequestDto)
        throws CustomIllegalArgumentException, CustomEntityNotFoundException {
        String toUserNickName = friendshipRequestDto.getToUserNickName();
        User fromUser = (User) authentication.getPrincipal();
        User toUser = userRepository.findByNickName(toUserNickName)
            .orElseThrow(() -> new CustomEntityNotFoundException(null, "Invalid nickname: " + toUserNickName));

        Friendship friendship = friendshipRepository.findByFromUserAndToUser(fromUser, toUser)
            .orElseThrow(() -> new CustomIllegalArgumentException(null, "Invalid friendship"));
        friendship.setFriendStatus(friendshipRequestDto.getFriendStatus());
        friendshipRepository.save(friendship);
    }

    public void deleteFriendship(Authentication authentication, String friendshipId)
        throws CustomIllegalArgumentException {
        User fromUser = (User) authentication.getPrincipal();
        Friendship friendship = friendshipRepository.findByIdAndFromUser(friendshipId, fromUser)
            .orElseThrow(() -> new CustomIllegalArgumentException(null, "Invalid friendship"));
        friendshipRepository.delete(friendship);
    }
}
