package discord.chat.gateway.infrastructure.user;

import java.util.Objects;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import discord.chat.gateway.infrastructure.common.BaseEntity;
import discord.chat.gateway.interfaces.user.UserRequest;

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
    @Field("password")
    private String password;

    @Setter
    @Field("image_url")
    private String imageUrl;

    public User(UserRequest userRequest) {
        this.nickName = userRequest.getNickName();
        this.email = userRequest.getEmail();
        this.password = userRequest.getPassword();
        this.imageUrl = userRequest.getImageUrl();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }
}

