package discord.chat.common.infrastructure.user;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserMongoRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);

    Optional<User> findByNickName(String email);
}
