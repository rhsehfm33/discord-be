package discord.chat.endpoint.test.integration;

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

import com.mongodb.DuplicateKeyException;

import discord.chat.endpoint.domain.user.UserService;
import discord.chat.endpoint.interfaces.user.UserRequest;
import discord.chat.endpoint.interfaces.user.UserResponse;
import discord.chat.endpoint.test.BaseIntegrationTest;
import discord.chat.endpoint.util.InstanceSetter;

public class UserIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    void testUserDuplication() throws NoSuchFieldException, IllegalAccessException {
        UserRequest userRequest = new UserRequest();
        // InstanceSetter.setField(userRequest, "email", "test_user@gmail.com");
        InstanceSetter.setField(userRequest, "nickName", "test_user");
        InstanceSetter.setField(userRequest, "password", "asdqwe12#");
        userService.createdUser(userRequest);
        Assertions.assertThrows(DuplicateKeyException.class, () -> {
            userService.createdUser(userRequest);
        });
    }

    // 사용자를 동시에 생성했을 때, 중복된 필드가 발생하면 에러가 발생해야 함
    @Test
    public void testConcurrentUserDuplication()
        throws NoSuchFieldException, IllegalAccessException {
        int numberOfThreads = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        List<Callable<UserResponse>> tasks = new ArrayList<>();
        for (int i = 0; i < numberOfThreads; ++i) {
            UserRequest userRequest = new UserRequest();
            InstanceSetter.setField(userRequest, "email", "test_user@gmail.com");
            InstanceSetter.setField(userRequest, "nickName", "test_user");
            InstanceSetter.setField(userRequest, "password", "asdqwe12#");
            tasks.add(() -> userService.createdUser(userRequest));
        }

        Assertions.assertThrows(ExecutionException.class, () -> {
            List<Future<UserResponse>> results = executorService.invokeAll(tasks);

            for (Future<UserResponse> result : results) {
                try {
                    result.get();
                } catch (ExecutionException e) {
                    throw e;
                }
            }
        });
    }
}
