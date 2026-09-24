package discord.chat.api.infrastructure.redis;

import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.codec.StringCodec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
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
    public StatefulRedisClusterConnection<String, String> redisClusterConnection(
        LettuceConnectionFactory lettuceConnectionFactory
    ) {
        return redisClusterClient(lettuceConnectionFactory).connect(StringCodec.UTF8);
    }

    @Bean
    public StatefulRedisClusterPubSubConnection<String, String> redisClusterPubSubConnection(
        LettuceConnectionFactory lettuceConnectionFactory
    ) {
        return redisClusterClient(lettuceConnectionFactory).connectPubSub(StringCodec.UTF8);
    }

    private RedisClusterClient redisClusterClient(LettuceConnectionFactory lettuceConnectionFactory) {
        if (lettuceConnectionFactory.getRequiredNativeClient() instanceof RedisClusterClient redisClusterClient) {
            return redisClusterClient;
        }
        throw new IllegalStateException("Redis Cluster must be configured");
    }
}
