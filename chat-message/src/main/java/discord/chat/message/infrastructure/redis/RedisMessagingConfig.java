package discord.chat.message.infrastructure.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class RedisMessagingConfig {
    @Bean
    public ThreadPoolTaskExecutor redisMessageExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("redis-message-");
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1000);
        return executor;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
        RedisConnectionFactory connectionFactory,
        ThreadPoolTaskExecutor redisMessageExecutor
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setMaxSubscriptionRegistrationWaitingTime(5000);
        container.setTaskExecutor(redisMessageExecutor);
        return container;
    }
}
