package team.discordbe.interfaces.friend.friendship;

import lombok.Getter;
import team.discordbe.infrastructure.user.User;
import team.discordbe.interfaces.user.SimpleUserResponseDto;

@Getter
public class FriendshipResponse {
    private final String friendshipId;
    private final SimpleUserResponseDto user;

    public FriendshipResponse(String friendshipId, User user) {
        this.friendshipId = friendshipId;
        this.user = new SimpleUserResponseDto(user);
    }
}
