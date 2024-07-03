package team.discordbe.domain.invitation.friend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import team.discordbe.domain.invitation.friend.model.FriendInvitation;
import team.discordbe.domain.user.model.User;

public interface FriendInvitationRepository extends MongoRepository<FriendInvitation, String> {
    Optional<FriendInvitation> findByFromUserAndToUser(User fromUser, User toUser);

    List<FriendInvitation> findByToUser(User toUser);
}
