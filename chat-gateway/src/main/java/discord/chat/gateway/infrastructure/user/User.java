package discord.chat.gateway.infrastructure.user;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import discord.chat.gateway.infrastructure.common.BaseEntity;
import discord.chat.gateway.interfaces.user.UserRequest;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User extends BaseEntity implements UserDetails {
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

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getUsername() {
        return id;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

