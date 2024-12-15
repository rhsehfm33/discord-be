package discord.chat.gateway.interfaces.user;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import discord.chat.gateway.infrastructure.user.User;

@Getter
@Builder
@AllArgsConstructor
public class UserResponse {
    private final String id;
    private final String email;
    private final String nickName;
    private final LocalDateTime createdAt;

    public UserResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.nickName = user.getNickName();
        this.createdAt = user.getCreatedAt();
    }
}
