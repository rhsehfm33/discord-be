package team.discordbe.domain.user.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import team.discordbe.domain.user.model.User;

@Getter
@Builder
@AllArgsConstructor
public class UserResponseDto {
    private final String id;
    private final String email;
    private final String nickName;
    private final LocalDateTime createdAt;

    public UserResponseDto(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.nickName = user.getNickName();
        this.createdAt = user.getCreatedAt();
    }
}
