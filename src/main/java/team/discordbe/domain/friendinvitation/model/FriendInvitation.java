package team.discordbe.domain.friendinvitation.model;

import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.discordbe.domain.user.model.User;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "friend_invitations")
public class FriendInvitation {
    @Id
    private String id;

    @DBRef
    private User fromUser;

    @DBRef
    private User toUser;

    public FriendInvitation(User fromUser, User toUser) {
        this.fromUser = fromUser;
        this.toUser = toUser;
    }
}
