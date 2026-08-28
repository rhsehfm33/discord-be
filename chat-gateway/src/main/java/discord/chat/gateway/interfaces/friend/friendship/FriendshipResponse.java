package discord.chat.gateway.interfaces.friend.friendship;

import lombok.Getter;
import discord.chat.common.infrastructure.user.User;
import discord.chat.gateway.interfaces.user.SimpleUserResponseDto;

@Getter
public class FriendshipResponse {
    private final String friendshipId;
    private final SimpleUserResponseDto user;

    public FriendshipResponse(String friendshipId, User user) {
        this.friendshipId = friendshipId;
        this.user = new SimpleUserResponseDto(user);
    }
}
