package discord.chat.gateway.interfaces.user;

import lombok.Getter;
import discord.chat.gateway.infrastructure.user.User;

@Getter
public class SimpleUserResponseDto {
    private final String nickName;

    public SimpleUserResponseDto(User user) {
        this.nickName = user.getNickName();
    }
}
