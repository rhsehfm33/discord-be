package team.discordbe.domain.user.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import team.discordbe.domain.user.model.User;

public interface UserRepository extends MongoRepository<User, Integer> {
    Optional<User> findByEmail(String email);
}
