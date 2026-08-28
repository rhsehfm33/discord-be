package discord.chat.common.infrastructure.chat.room;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import discord.chat.common.infrastructure.user.User;

public interface ChatRoomMongoRepository extends MongoRepository<ChatRoom, String> {
    Optional<ChatRoom> findByIdAndOwner(String id, User owner);

    Optional<ChatRoom> findByIdAndType(String id, ChatRoomType type);
}
