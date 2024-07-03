package team.discordbe.domain.user.dto;

import lombok.Getter;
import team.discordbe.domain.user.model.User;

@Getter
public class SimpleUserResponseDto {
    private final String nickName;

    public SimpleUserResponseDto(User user) {
        this.nickName = user.getNickName();
    }
}
