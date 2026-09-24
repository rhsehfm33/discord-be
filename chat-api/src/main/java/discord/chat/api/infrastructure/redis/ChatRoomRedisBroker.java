package discord.chat.api.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import discord.chat.api.application.session.ChatSessionEvent;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.pubsub.RedisClusterPubSubAdapter;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ChatRoomRedisBroker {
    private static final String TOPIC_SUFFIX = ":session-events";

    private final StatefulRedisClusterConnection<String, String> redisClusterConnection;
    private final StatefulRedisClusterPubSubConnection<String, String> redisClusterPubSubConnection;
    private final ThreadPoolTaskExecutor redisMessageExecutor;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final String topic;

    public ChatRoomRedisBroker(
        StatefulRedisClusterConnection<String, String> redisClusterConnection,
        StatefulRedisClusterPubSubConnection<String, String> redisClusterPubSubConnection,
        ThreadPoolTaskExecutor redisMessageExecutor,
        ObjectMapper objectMapper,
        ApplicationEventPublisher applicationEventPublisher,
        @Value("${messaging.redis.topic-prefix}") String topicPrefix
    ) {
        this.redisClusterConnection = redisClusterConnection;
        this.redisClusterPubSubConnection = redisClusterPubSubConnection;
        this.redisMessageExecutor = redisMessageExecutor;
        this.objectMapper = objectMapper;
        this.applicationEventPublisher = applicationEventPublisher;
        this.topic = topicPrefix + TOPIC_SUFFIX;
        this.redisClusterPubSubConnection.addListener(new RedisClusterPubSubAdapter<>() {
            @Override
            public void smessage(RedisClusterNode redisClusterNode, String channel, String message) {
                if (topic.equals(channel)) {
                    redisMessageExecutor.execute(() -> onMessage(message));
                }
            }
        });
    }

    @PostConstruct
    public void subscribe() {
        redisClusterPubSubConnection.sync().ssubscribe(topic);
    }

    @PreDestroy
    public void unsubscribe() {
        redisClusterPubSubConnection.sync().sunsubscribe(topic);
    }

    public void publish(ChatSessionEvent event) throws JsonProcessingException {
        long subscriberCount = redisClusterConnection.sync().spublish(topic, objectMapper.writeValueAsString(event));
        if (subscriberCount == 0) {
            throw new IllegalStateException("No Redis subscribers for chat session events");
        }
    }

    private void onMessage(String message) {
        try {
            ChatSessionEvent event = objectMapper.readValue(message, ChatSessionEvent.class);
            applicationEventPublisher.publishEvent(event);
        } catch (Exception exception) {
            log.error("Redis chat session event handling failed", exception);
        }
    }
}
