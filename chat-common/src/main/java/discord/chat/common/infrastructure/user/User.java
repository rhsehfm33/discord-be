package discord.chat.common.infrastructure.user;

import java.util.Objects;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import discord.chat.common.infrastructure.common.BaseEntity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    public User(String nickName, String email, String password, String imageUrl) {
        this.nickName = nickName;
        this.email = email;
        this.password = password;
        this.imageUrl = imageUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }
}

