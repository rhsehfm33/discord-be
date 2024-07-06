package team.discordbe.domain.friend.friendship.model;

import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import team.discordbe.domain.friend.friendship.constant.FriendStatus;
import team.discordbe.domain.user.model.User;

@Getter
@NoArgsConstructor
@AllArgsConstructor
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
