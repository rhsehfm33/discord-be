package discord.chat.endpoint.infrastructure.chat.channel;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import discord.chat.endpoint.infrastructure.chat.room.ChatRoom;
import discord.chat.endpoint.infrastructure.user.User;

public interface TextChannelMongoRepository extends MongoRepository<TextChannel, String> {
    void deleteAllByChatRoom(ChatRoom chatRoom);

    void deleteByOwner(User owner);

    List<TextChannel> findAllByChatRoom(ChatRoom chatRoom);
}
