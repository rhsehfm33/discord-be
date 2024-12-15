package discord.chat.endpoint.infrastructure.friend.friendship;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import discord.chat.endpoint.infrastructure.user.User;

public interface FriendshipMongoRepository extends MongoRepository<Friendship, String> {
    Optional<Friendship> findByFromUserAndToUser(User fromUser, User toUser);

    Optional<Friendship> findByIdAndFromUser(String id, User fromUser);
}
