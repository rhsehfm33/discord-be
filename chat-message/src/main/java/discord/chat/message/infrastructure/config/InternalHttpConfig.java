package discord.chat.message.infrastructure.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class InternalHttpConfig {
    @Bean
    public RestTemplate internalRestTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
