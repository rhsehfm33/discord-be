package discord.chat.api.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import discord.chat.api.interfaces.message.ChatMessageResponse;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.pubsub.RedisClusterPubSubAdapter;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ChatMessageRedisBroker {
    private final StatefulRedisClusterConnection<String, String> redisClusterConnection;
    private final StatefulRedisClusterPubSubConnection<String, String> redisClusterPubSubConnection;
    private final ThreadPoolTaskExecutor redisMessageExecutor;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher events;
    private final String topicPrefix;

    public ChatMessageRedisBroker(
        StatefulRedisClusterConnection<String, String> redisClusterConnection,
        StatefulRedisClusterPubSubConnection<String, String> redisClusterPubSubConnection,
        ThreadPoolTaskExecutor redisMessageExecutor,
        ObjectMapper objectMapper,
        ApplicationEventPublisher events,
        @Value("${messaging.redis.topic-prefix}") String topicPrefix
    ) {
        this.redisClusterConnection = redisClusterConnection;
        this.redisClusterPubSubConnection = redisClusterPubSubConnection;
        this.redisMessageExecutor = redisMessageExecutor;
        this.objectMapper = objectMapper;
        this.events = events;
        this.topicPrefix = topicPrefix;
        this.redisClusterPubSubConnection.addListener(new RedisClusterPubSubAdapter<>() {
            @Override
            public void smessage(RedisClusterNode redisClusterNode, String channel, String message) {
                if (!channel.equals(topicPrefix + ":session-events")) {
                    redisMessageExecutor.execute(() -> onMessage(channel, message));
                }
            }
        });
    }

    public void subscribe(String chatRoomId) {
        try {
            redisClusterPubSubConnection.sync().ssubscribe(topic(chatRoomId));
            requireConnection();
        } catch (RuntimeException exception) {
            unsubscribe(chatRoomId);
            throw new IllegalStateException("Could not subscribe to chat room " + chatRoomId, exception);
        }
    }

    public void requireConnection() {
        if (!redisClusterConnection.isOpen() || !redisClusterPubSubConnection.isOpen()) {
            throw new IllegalStateException("Redis Cluster connection is unavailable");
        }
    }

    public void unsubscribe(String chatRoomId) {
        try {
            redisClusterPubSubConnection.sync().sunsubscribe(topic(chatRoomId));
        } catch (RuntimeException exception) {
            log.error("Redis shard unsubscribe failed: chatRoomId={}", chatRoomId, exception);
        }
    }

    public void publish(ChatMessageResponse message) throws JsonProcessingException {
        long subscribers = redisClusterConnection.sync().spublish(
            topic(message.getChatRoomId()), objectMapper.writeValueAsString(message)
        );
        if (subscribers == 0) {
            throw new IllegalStateException("No Redis subscribers for chat room " + message.getChatRoomId());
        }
    }

    private void onMessage(String receivedTopic, String message) {
        try {
            ChatMessageResponse response = objectMapper.readValue(message, ChatMessageResponse.class);
            if (!receivedTopic.equals(topic(response.getChatRoomId()))) {
                throw new IllegalArgumentException("Redis topic does not match message chat room");
            }
            events.publishEvent(response);
        } catch (Exception exception) {
            log.error("Redis chat message delivery failed", exception);
        }
    }

    private String topic(String chatRoomId) {
        return topicPrefix + ":" + chatRoomId;
    }
}
