package discord.chat.api.interfaces.user;

import discord.chat.api.infrastructure.user.User;
import lombok.Getter;

@Getter
public class SimpleUserResponseDto {
    private final String nickName;

    public SimpleUserResponseDto(User user) {
        this.nickName = user.getNickName();
    }
}
