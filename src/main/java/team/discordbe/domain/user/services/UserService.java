package team.discordbe.domain.user.services;

import java.util.Collections;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import team.discordbe.domain.user.dto.UserRequestDto;
import team.discordbe.domain.user.dto.UserResponseDto;
import team.discordbe.domain.user.model.User;
import team.discordbe.domain.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email).orElseThrow(() ->
            new EntityNotFoundException("User not found with email : " + email));

        return new org.springframework.security.core.userdetails.User(
            user.getEmail(), user.getPassword(), Collections.emptyList());
    }

    public UserResponseDto createdUser(UserRequestDto dto) {
        dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        User user = userRepository.save(new User(dto));
        return new UserResponseDto(user);
    }

    public UserResponseDto updateUser(Integer id, UserRequestDto dto) {
        User targetUser = userRepository.findById(id).orElseThrow(() ->
            new EntityNotFoundException("User not found with id : " + id));

        targetUser.setNickName(dto.getNickName());
        targetUser.setImageUrl(dto.getEmail());
        return new UserResponseDto(userRepository.save(targetUser));
    }

    public void deleteUser(Integer id) {
        userRepository.deleteById(id);
    }
}
