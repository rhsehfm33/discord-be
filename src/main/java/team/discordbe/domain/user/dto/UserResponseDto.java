package team.discordbe.domain.user.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import team.discordbe.domain.user.model.User;

@Getter
public class UserResponseDto {
    private String id;
    private String email;
    private String nickName;
    private LocalDateTime createdAt;

    public UserResponseDto(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.nickName = user.getNickName();
        this.createdAt = user.getCreatedAt();
    }
}
