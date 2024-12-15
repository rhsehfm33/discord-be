package discord.chat.gateway.infrastructure.chat.subsription;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import discord.chat.gateway.infrastructure.chat.room.ChatRoom;
import discord.chat.gateway.infrastructure.user.User;

public interface ChatSubscriptMongoRepository extends MongoRepository<ChatSubscription, String> {
    Optional<ChatSubscription> findByUserAndChatRoom(User user, ChatRoom chatRoom);

    void deleteAllByChatRoom(ChatRoom chatRoom);
}
