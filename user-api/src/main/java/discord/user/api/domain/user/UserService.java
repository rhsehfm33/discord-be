package discord.user.api.domain.user;

import discord.user.api.interfaces.user.UserRequest;
import discord.user.api.interfaces.user.UserResponse;
import discord.chat.common.exception.CustomEntityNotFoundException;
import discord.chat.common.infrastructure.user.User;
import discord.chat.common.infrastructure.user.UserMongoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserMongoRepository userMongoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userMongoRepository.findByEmail(email).orElseThrow(() ->
            new EntityNotFoundException("Wrong user info"));

        return new org.springframework.security.core.userdetails.User(
            user.getEmail(), user.getPassword(), Collections.emptyList());
    }

    public User getUserByEmail(String email) {
        return userMongoRepository.findByEmail(email).orElseThrow(() ->
            new EntityNotFoundException("Wrong user info"));
    }

    public UserResponse createdUser(UserRequest dto) {
        dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        User user = userMongoRepository.save(
            new User(dto.getNickName(), dto.getEmail(), dto.getPassword(), dto.getImageUrl())
        );
        return new UserResponse(user);
    }

    @PreAuthorize("isAuthenticated()")
    public UserResponse getMyUserInfo(Authentication authentication) {
        String userId = ((User) authentication.getPrincipal()).getId();
        User user = userMongoRepository.findById(userId).orElseThrow(() ->
            new EntityNotFoundException("Wrong user info"));
        return new UserResponse(user);
    }

    public UserResponse updateUser(String id, UserRequest dto)
        throws CustomEntityNotFoundException {
        User targetUser = userMongoRepository.findById(id).orElseThrow(() ->
            new CustomEntityNotFoundException(null, "User not found with id : " + id));

        targetUser.setNickName(dto.getNickName());
        targetUser.setImageUrl(dto.getEmail());
        return new UserResponse(userMongoRepository.save(targetUser));
    }

    public void deleteUser(String id) {
        userMongoRepository.deleteById(id);
    }
}

