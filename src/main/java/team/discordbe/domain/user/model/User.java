package team.discordbe.domain.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.discordbe.domain.user.dto.UserRequestDto;
import team.discordbe.global.base.BaseEntity;

@Entity
@Table(name= "USERS")
@Getter
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Column(name = "nick_name", unique = true, nullable = false, length = 50)
    private String nickName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "image_url")
    private String imageUrl;

    public void setUser(String nickName, String imageUrl, String password) {
        this.nickName = nickName;
        this.imageUrl = imageUrl;
        this.password = password;
    }

    public static User from(UserRequestDto dto) {
        return User.builder()
                .email(dto.getEmail())
                .nickName(dto.getNickName())
                .password(dto.getPassword())
                .build();
    }
}

