package team.discordbe.interfaces.friend.friendship;

import lombok.AllArgsConstructor;
import lombok.Getter;
import team.discordbe.domain.friend.friendship.FriendStatus;

@Getter
@AllArgsConstructor
public class FriendshipRequest {
    private final String toUserNickName;
    private final FriendStatus friendStatus;
}
