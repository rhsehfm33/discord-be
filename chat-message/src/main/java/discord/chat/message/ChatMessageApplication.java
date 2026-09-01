package discord.chat.message;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(
    scanBasePackages = {"discord.chat.message", "discord.chat.common"},
    exclude = UserDetailsServiceAutoConfiguration.class
)
@EnableMongoRepositories(basePackages = "discord.chat.message.infrastructure")
@EntityScan(basePackages = "discord.chat.common.infrastructure")
public class ChatMessageApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatMessageApplication.class, args);
    }

}

