package team.discordbe.domain.user.dto;

import lombok.Builder;
import lombok.Getter;
import team.discordbe.domain.user.model.User;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponseDto {
    private Long id;
    private String email;
    private String nickName;
    private LocalDateTime createdAt;

    public static UserResponseDto from(User user) {
        return UserResponseDto.builder().
                id(user.getId()).
                nickName(user.getNickName()).
                email(user.getEmail()).
                createdAt(user.getCreatedAt()).
                build();
    }
}
