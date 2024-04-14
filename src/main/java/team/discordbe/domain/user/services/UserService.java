package team.discordbe.domain.user.services;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import team.discordbe.domain.user.dto.UserRequestDto;
import team.discordbe.domain.user.dto.UserResponseDto;
import team.discordbe.domain.user.model.User;
import team.discordbe.domain.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    public UserResponseDto createdUser(UserRequestDto dto) {
        User user = this.userRepository.save(User.from(dto));
        return UserResponseDto.from(user);
    }

    public UserResponseDto updateUser(Integer id, UserRequestDto dto) {
        User findedUser = this.userRepository.findById(id).orElseThrow();
        findedUser.setUser(dto.getNickName(), dto.getImageUrl(), dto.getPassword());
        return UserResponseDto.from(this.userRepository.save(findedUser));
    }

    public void deleteUser(Integer id) {
        this.userRepository.deleteById(id);
    }
}
