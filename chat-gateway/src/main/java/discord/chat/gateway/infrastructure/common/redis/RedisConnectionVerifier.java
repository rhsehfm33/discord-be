package discord.chat.gateway.infrastructure.common.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component
public class RedisConnectionVerifier implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(RedisConnectionVerifier.class);

    private final RedisConnectionFactory connectionFactory;

    public RedisConnectionVerifier(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            String response = connection.ping();

            if (!"PONG".equals(response)) {
                throw new IllegalStateException("Unexpected Redis PING response: " + response);
            }

            logger.info("Redis connection established");
        } catch (Exception exception) {
            logger.error("Redis connection failed", exception);
            throw new IllegalStateException("Redis connection verification failed", exception);
        }
    }
}
