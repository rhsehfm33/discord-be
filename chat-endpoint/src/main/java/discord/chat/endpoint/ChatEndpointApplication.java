package discord.chat.endpoint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = {"discord.chat.endpoint", "discord.chat.common"})
@EnableMongoRepositories(basePackages = "discord.chat.common.infrastructure")
@EntityScan(basePackages = "discord.chat.common.infrastructure")
public class ChatEndpointApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatEndpointApplication.class, args);
    }

}
