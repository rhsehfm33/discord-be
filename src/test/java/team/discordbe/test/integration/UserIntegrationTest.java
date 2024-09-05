package team.discordbe.test.integration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import team.discordbe.domain.user.dto.UserRequestDto;
import team.discordbe.domain.user.dto.UserResponseDto;
import team.discordbe.domain.user.services.UserService;
import team.discordbe.util.InstanceSetter;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class UserIntegrationTest {
    @Autowired
    private UserService userService;

    @Test
    void testCreateUser() throws NoSuchFieldException, IllegalAccessException {
        UserRequestDto userRequestDto = new UserRequestDto();
        InstanceSetter.setField(userRequestDto, "email", "test_user@gmail.com");
        InstanceSetter.setField(userRequestDto, "nickName", "test_user");
        InstanceSetter.setField(userRequestDto, "password", "asdqwe12#");
        UserResponseDto responseDto = userService.createdUser(userRequestDto);
        Assertions.assertEquals(responseDto.getEmail(), "test_user@gmail.com");
    }
}
