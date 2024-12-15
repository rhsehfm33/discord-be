package discord.chat.endpoint.interfaces.friend.friendship;

import lombok.Getter;
import discord.chat.endpoint.infrastructure.user.User;
import discord.chat.endpoint.interfaces.user.SimpleUserResponseDto;

@Getter
public class FriendshipResponse {
    private final String friendshipId;
    private final SimpleUserResponseDto user;

    public FriendshipResponse(String friendshipId, User user) {
        this.friendshipId = friendshipId;
        this.user = new SimpleUserResponseDto(user);
    }
}
