package team.discordbe.infrastructure.chat.channel;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import team.discordbe.infrastructure.chat.room.ChatRoom;
import team.discordbe.infrastructure.user.User;

public interface TextChannelMongoRepository extends MongoRepository<TextChannel, String> {
    void deleteAllByChatRoom(ChatRoom chatRoom);

    void deleteByOwner(User owner);

    List<TextChannel> findAllByChatRoom(ChatRoom chatRoom);
}
