package discord.chat.common.infrastructure.chat.subsription;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import discord.chat.common.infrastructure.chat.room.ChatRoom;
import discord.chat.common.infrastructure.user.User;

public interface ChatSubscriptMongoRepository extends MongoRepository<ChatSubscription, String> {
    List<ChatSubscription> findAllByUser(User user);

    Optional<ChatSubscription> findByUserAndChatRoom(User user, ChatRoom chatRoom);

    void deleteAllByChatRoom(ChatRoom chatRoom);
}
