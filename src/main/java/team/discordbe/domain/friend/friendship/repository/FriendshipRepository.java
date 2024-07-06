package team.discordbe.domain.friend.friendship.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import team.discordbe.domain.friend.friendship.model.Friendship;
import team.discordbe.domain.user.model.User;

public interface FriendshipRepository extends MongoRepository<Friendship, String> {
    Optional<Friendship> findByFromUserAndToUser(User fromUser, User toUser);

    Optional<Friendship> findByIdAndFromUser(String id, User fromUser);
}
