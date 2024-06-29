package team.discordbe.domain.user.model;

import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import team.discordbe.domain.user.dto.UserRequestDto;
import team.discordbe.global.base.BaseEntity;

@Document
@Getter
public class User extends BaseEntity {
    @Id
    private String id;

    @Setter
    @Column(name = "nick_name", unique = true, nullable = false, length = 50)
    private String nickName;

    @Setter
    @Column(unique = true, nullable = false)
    private String email;

    @Setter
    @Column(name = "password")
    private String password;

    @Setter
    @Column(name = "image_url")
    private String imageUrl;

    public User(UserRequestDto userRequestDto) {
        this.nickName = userRequestDto.getNickName();
        this.email = userRequestDto.getEmail();
        this.password = userRequestDto.getPassword();
        this.imageUrl = userRequestDto.getImageUrl();
    }
}

