package team.discordbe.domain.invitation.friend.model;

import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Id;
import lombok.Getter;
import team.discordbe.domain.user.model.User;

@Getter
@Document(collection = "friend_invitations")
public class FriendInvitation {
    @Id
    private String id;

    @DBRef
    private final User fromUser;

    @DBRef
    private final User toUser;

    public FriendInvitation(User fromUser, User toUser) {
        this.fromUser = fromUser;
        this.toUser = toUser;
    }
}
