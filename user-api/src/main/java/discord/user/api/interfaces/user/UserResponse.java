package discord.user.api.interfaces.user;

import java.time.LocalDateTime;

import discord.chat.common.infrastructure.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserResponse {
    private final String id;
    private final String email;
    private final String nickName;
    private final String imageUrl;
    private final LocalDateTime createdAt;

    public UserResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.nickName = user.getNickName();
        this.imageUrl = user.getImageUrl();
        this.createdAt = user.getCreatedAt();
    }
}

