package discord.chat.endpoint.infrastructure.chat.subsription;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import discord.chat.endpoint.infrastructure.chat.room.ChatRoom;
import discord.chat.endpoint.infrastructure.user.User;

public interface ChatSubscriptMongoRepository extends MongoRepository<ChatSubscription, String> {
    Optional<ChatSubscription> findByUserAndChatRoom(User user, ChatRoom chatRoom);

    void deleteAllByChatRoom(ChatRoom chatRoom);
}
