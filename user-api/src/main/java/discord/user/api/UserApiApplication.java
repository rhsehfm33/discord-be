package discord.user.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = {"discord.user.api", "discord.chat.common"})
@EnableMongoRepositories(basePackages = "discord.chat.common.infrastructure")
@EntityScan(basePackages = "discord.chat.common.infrastructure")
public class UserApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApiApplication.class, args);
    }

}

