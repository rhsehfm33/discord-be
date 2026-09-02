package discord.chat.message.infrastructure.message;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface ChatMessageMongoRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findByChatRoomIdAndTextChannelId(
        String chatRoomId,
        String textChannelId,
        Pageable pageable
    );

    @Query("{ 'chat_room_id': ?0, 'text_channel_id': ?1, '_id': { '$lt': ?2 } }")
    List<ChatMessage> findMessagesBefore(
        String chatRoomId,
        String textChannelId,
        ObjectId id,
        Pageable pageable
    );
}
