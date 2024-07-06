package team.discordbe.domain.friend.friendship.dto;

import lombok.Getter;
import team.discordbe.domain.user.dto.SimpleUserResponseDto;
import team.discordbe.domain.user.model.User;

@Getter
public class FriendshipResponseDto {
    private final String friendshipId;
    private final SimpleUserResponseDto user;

    public FriendshipResponseDto(String friendshipId, User user) {
        this.friendshipId = friendshipId;
        this.user = new SimpleUserResponseDto(user);
    }
}
