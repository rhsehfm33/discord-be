package discord.chat.common.infrastructure.chat.directmessage;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface DirectMessageMongoRepository extends MongoRepository<DirectMessage, String> {
}
