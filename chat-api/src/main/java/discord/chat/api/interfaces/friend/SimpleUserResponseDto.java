package discord.chat.api.interfaces.friend;

import discord.chat.common.infrastructure.user.User;
import lombok.Getter;

@Getter
public class SimpleUserResponseDto {
    private final String nickName;
    private final String imageUrl;

    public SimpleUserResponseDto(User user) {
        this.nickName = user.getNickName();
        this.imageUrl = user.getImageUrl();
    }
}

