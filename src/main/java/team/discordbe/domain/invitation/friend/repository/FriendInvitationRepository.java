package team.discordbe.domain.invitation.friend.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import team.discordbe.domain.invitation.friend.model.FriendInvitation;
import team.discordbe.domain.user.model.User;

public interface FriendInvitationRepository extends MongoRepository<FriendInvitation, String> {
    Optional<FriendInvitation> findByFromUserAndToUser(User fromUser, User toUser);

    Optional<FriendInvitation> findByIdAndFromUser(String id, User fromUser);

    Optional<FriendInvitation> findByIdAndToUser(String id, User toUser);
}
