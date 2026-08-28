package discord.chat.common.infrastructure.chat.message;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatMessageMongoRepository extends MongoRepository<ChatMessage, String> {
    
}
