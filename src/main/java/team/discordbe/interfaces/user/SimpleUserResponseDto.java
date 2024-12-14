package team.discordbe.interfaces.user;

import lombok.Getter;
import team.discordbe.infrastructure.user.User;

@Getter
public class SimpleUserResponseDto {
    private final String nickName;

    public SimpleUserResponseDto(User user) {
        this.nickName = user.getNickName();
    }
}
