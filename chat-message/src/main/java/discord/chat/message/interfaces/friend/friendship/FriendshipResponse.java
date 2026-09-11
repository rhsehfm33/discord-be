package discord.chat.message.interfaces.friend.friendship;

import discord.chat.common.infrastructure.user.User;
import discord.chat.message.interfaces.friend.SimpleUserResponseDto;
import lombok.Getter;

@Getter
public class FriendshipResponse {
    private final String friendshipId;
    private final SimpleUserResponseDto user;

    public FriendshipResponse(String friendshipId, User user) {
        this.friendshipId = friendshipId;
        this.user = new SimpleUserResponseDto(user);
    }
}

