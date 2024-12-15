package discord.chat.endpoint.interfaces.friend.friendship;

import lombok.AllArgsConstructor;
import lombok.Getter;
import discord.chat.endpoint.domain.friend.friendship.FriendStatus;

@Getter
@AllArgsConstructor
public class FriendshipRequest {
    private final String toUserNickName;
    private final FriendStatus friendStatus;
}
