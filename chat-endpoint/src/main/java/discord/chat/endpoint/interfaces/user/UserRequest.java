package discord.chat.endpoint.interfaces.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
public class UserRequest {
    @Email(message="이메일 형식이 아닙니다.")
    @Size(min=7, max=30)
    private String email;

    @Size(min=3, max=12)
    private String nickName;

    @Setter
    @Size(min=7, max=20)
    private String password;

    private String imageUrl;
}

