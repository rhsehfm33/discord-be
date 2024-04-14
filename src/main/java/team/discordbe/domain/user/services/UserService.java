package team.discordbe.domain.user.services;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import team.discordbe.domain.user.dto.UserRequestDto;
import team.discordbe.domain.user.dto.UserResponseDto;
import team.discordbe.domain.user.model.User;
import team.discordbe.domain.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponseDto createdUser(UserRequestDto dto) {
        dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        User user = this.userRepository.save(User.from(dto));
        return UserResponseDto.from(user);
    }

    public UserResponseDto updateUser(Integer id, UserRequestDto dto) {
        User findedUser = this.userRepository.findById(id).orElseThrow();
/*
         try catch 적용 예정
         패스워드가 비어있지 않은가?
         예정 ) 비밀번호 변경은 지금도 가능하지만 나중에 적용 예정
         if(dto.getPassword() != null) {
            dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
*/
        findedUser.setUser(dto.getNickName(), dto.getImageUrl(), null);
        return UserResponseDto.from(this.userRepository.save(findedUser));
    }

    public void deleteUser(Integer id) {
        this.userRepository.deleteById(id);
    }
}
