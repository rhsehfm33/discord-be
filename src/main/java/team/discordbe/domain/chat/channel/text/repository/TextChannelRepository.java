package team.discordbe.domain.chat.channel.text.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import team.discordbe.domain.chat.channel.text.model.TextChannel;
import team.discordbe.domain.chat.room.model.ChatRoom;
import team.discordbe.domain.user.model.User;

public interface TextChannelRepository extends MongoRepository<TextChannel, String> {
    void deleteAllByChatRoom(ChatRoom chatRoom);

    void deleteByOwner(User owner);

    List<TextChannel> findAllByChatRoom(ChatRoom chatRoom);
}
