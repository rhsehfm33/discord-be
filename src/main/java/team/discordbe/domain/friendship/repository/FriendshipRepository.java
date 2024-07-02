package team.discordbe.domain.friendship.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import team.discordbe.domain.friendship.constant.FriendStatus;
import team.discordbe.domain.friendship.model.Friendship;

@Repository
public interface FriendshipRepository extends MongoRepository<Friendship, String> {
    Optional<Friendship> findByFromUserIdAndToUserId(String fromUserId, String toUserId);

    List<Friendship> findByFromUserIdAndFriendStatus(String fromUserId, FriendStatus friendStatus);

    List<Friendship> findByToUserIdAndFriendStatus(String toUserId, FriendStatus friendStatus);
}
