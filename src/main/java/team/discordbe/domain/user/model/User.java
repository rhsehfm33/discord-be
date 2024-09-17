package team.discordbe.domain.user.model;

import java.util.Objects;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import team.discordbe.domain.user.dto.UserRequestDto;
import team.discordbe.global.base.BaseEntity;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User extends BaseEntity {
    @Id
    private String id;

    @Setter
    @Indexed(unique = true)
    private String nickName;

    @Setter
    @Indexed(unique = true)
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }
}

