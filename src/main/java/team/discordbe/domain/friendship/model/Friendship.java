package team.discordbe.domain.friendship.model;

import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import team.discordbe.domain.friendship.constant.FriendStatus;

@Getter
@Document
public class Friendship {
    @Id
    private String id;

    private final String fromUserId;

    private final String toUserId;

    @Setter
    private FriendStatus friendStatus;

    public Friendship(String fromUserId, String toUserId, FriendStatus friendStatus) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.friendStatus = friendStatus;
    }
}
