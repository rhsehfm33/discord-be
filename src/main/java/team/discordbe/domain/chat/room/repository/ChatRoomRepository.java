package team.discordbe.domain.chat.room.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import team.discordbe.domain.chat.room.model.ChatRoom;
import team.discordbe.domain.user.model.User;

public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {
    Optional<ChatRoom> findByIdAndOwner(String id, User owner);
}
