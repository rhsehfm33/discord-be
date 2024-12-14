package team.discordbe.infrastructure.chat.subsription;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import team.discordbe.infrastructure.chat.room.ChatRoom;
import team.discordbe.infrastructure.user.User;

public interface ChatSubscriptMongoRepository extends MongoRepository<ChatSubscription, String> {
    Optional<ChatSubscription> findByUserAndChatRoom(User user, ChatRoom chatRoom);

    void deleteAllByChatRoom(ChatRoom chatRoom);
}
