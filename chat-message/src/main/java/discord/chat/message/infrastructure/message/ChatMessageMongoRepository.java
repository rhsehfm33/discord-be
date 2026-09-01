package discord.chat.message.infrastructure.message;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatMessageMongoRepository extends MongoRepository<ChatMessage, String> {
}
