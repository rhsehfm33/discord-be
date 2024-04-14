package team.discordbe.domain.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.discordbe.domain.user.dto.UserRequestDto;
import team.discordbe.global.base.BaseEntity;

@Entity
@Table(name= "USERS")
@Getter
@Builder
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

    @Column(name = "hashed_password")
    private String hashedPassword;

    @Column(name = "image_url")
    private String imageUrl;

    public void setUser(String nickName, String imageUrl, String password) {
        this.nickName = nickName;
        this.imageUrl = imageUrl;
        this.hashedPassword = password;
    }

    public static User from(UserRequestDto dto) {
        return User.builder()
                .email(dto.getEmail())
                .nickName(dto.getNickName())
                .hashedPassword(dto.getPassword())
                .build();
    }
}

