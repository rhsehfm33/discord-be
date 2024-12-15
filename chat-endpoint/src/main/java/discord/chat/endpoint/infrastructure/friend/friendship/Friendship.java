package discord.chat.endpoint.infrastructure.friend.friendship;

import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import discord.chat.endpoint.domain.friend.friendship.FriendStatus;
import discord.chat.endpoint.infrastructure.user.User;

@Getter
@NoArgsConstructor
@Document(collection = "friendships")
public class Friendship {
    @Id
    private String id;

    @DBRef
    private User fromUser;

    @DBRef
    private User toUser;

    @Setter
    private FriendStatus friendStatus;

    public Friendship(User fromUser, User toUser, FriendStatus friendStatus) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.friendStatus = friendStatus;
    }
}
