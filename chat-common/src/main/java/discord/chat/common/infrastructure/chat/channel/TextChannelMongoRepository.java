package discord.chat.common.infrastructure.chat.channel;

import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import discord.chat.common.infrastructure.chat.room.ChatRoom;
import discord.chat.common.infrastructure.user.User;

public interface TextChannelMongoRepository extends MongoRepository<TextChannel, String> {
    boolean existsByIdAndChatRoomId(String textChannelId, String chatRoomId);

    void deleteAllByChatRoom(ChatRoom chatRoom);

    void deleteByOwner(User owner);

    List<TextChannel> findAllByChatRoom(ChatRoom chatRoom);

    List<TextChannel> findAllByChatRoomId(String chatRoomId);

    List<TextChannel> findAllByChatRoomIn(Collection<ChatRoom> chatRooms);
}
