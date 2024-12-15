package discord.chat.api.infrastructure.friend.invitation;

import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import discord.chat.api.infrastructure.user.User;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
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
