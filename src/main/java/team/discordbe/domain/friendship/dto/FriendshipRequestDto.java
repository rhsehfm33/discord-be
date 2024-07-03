package team.discordbe.domain.friendship.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import team.discordbe.domain.friendship.constant.FriendStatus;

@Getter
@AllArgsConstructor
public class FriendshipRequestDto {
    private final String toUserNickName;
    private final FriendStatus friendStatus;
}
