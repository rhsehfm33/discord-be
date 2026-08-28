package discord.chat.api.interfaces.friend.friendship;

import discord.chat.common.infrastructure.friend.friendship.FriendStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FriendshipRequest {
    private final String toUserNickName;
    private final FriendStatus friendStatus;
}
