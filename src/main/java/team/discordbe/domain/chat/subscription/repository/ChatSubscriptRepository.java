package team.discordbe.domain.chat.subscription.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import team.discordbe.domain.chat.room.model.ChatRoom;
import team.discordbe.domain.chat.subscription.model.ChatSubscription;
import team.discordbe.domain.user.model.User;

public interface ChatSubscriptRepository extends MongoRepository<ChatSubscription, String> {
    Optional<ChatSubscription> findByUserAndChatRoom(User user, ChatRoom chatRoom);

    void deleteAllByChatRoom(ChatRoom chatRoom);
}
