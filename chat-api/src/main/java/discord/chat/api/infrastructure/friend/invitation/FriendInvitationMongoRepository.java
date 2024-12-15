package discord.chat.api.infrastructure.friend.invitation;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import discord.chat.api.infrastructure.user.User;

public interface FriendInvitationMongoRepository extends MongoRepository<FriendInvitation, String> {
    Optional<FriendInvitation> findByFromUserAndToUser(User fromUser, User toUser);

    Optional<FriendInvitation> findByIdAndFromUser(String id, User fromUser);

    Optional<FriendInvitation> findByIdAndToUser(String id, User toUser);
}
