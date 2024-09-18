package team.discordbe.test.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import team.discordbe.domain.user.dto.UserRequestDto;
import team.discordbe.domain.user.dto.UserResponseDto;
import team.discordbe.domain.user.services.UserService;
import team.discordbe.test.BaseIntegrationTest;
import team.discordbe.util.InstanceSetter;

public class UserIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    void testUserDuplication() throws NoSuchFieldException, IllegalAccessException {
        UserRequestDto userRequestDto = new UserRequestDto();
        InstanceSetter.setField(userRequestDto, "email", "test_user@gmail.com");
        InstanceSetter.setField(userRequestDto, "nickName", "test_user");
        InstanceSetter.setField(userRequestDto, "password", "asdqwe12#");
        userService.createdUser(userRequestDto);
        Assertions.assertThrows(DuplicateKeyException.class, () -> {
            userService.createdUser(userRequestDto);
        });
    }

    // 사용자를 동시에 생성했을 때, 중복된 필드가 발생하면 에러가 발생해야 함
    @Test
    public void testConcurrentUserDuplication()
        throws NoSuchFieldException, IllegalAccessException {
        int numberOfThreads = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        List<Callable<UserResponseDto>> tasks = new ArrayList<>();
        for (int i = 0; i < numberOfThreads; ++i) {
            UserRequestDto userRequestDto = new UserRequestDto();
            InstanceSetter.setField(userRequestDto, "email", "test_user@gmail.com");
            InstanceSetter.setField(userRequestDto, "nickName", "test_user");
            InstanceSetter.setField(userRequestDto, "password", "asdqwe12#");
            tasks.add(() -> userService.createdUser(userRequestDto));
        }

        Assertions.assertThrows(ExecutionException.class, () -> {
            List<Future<UserResponseDto>> results = executorService.invokeAll(tasks);

            for (Future<UserResponseDto> result : results) {
                try {
                    result.get();
                } catch (ExecutionException e) {
                    throw e;
                }
            }
        });
    }
}
