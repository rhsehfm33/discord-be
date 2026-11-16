package discord.chat.api.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import discord.chat.api.application.message.DirectMessageDelivery.DirectMessageReceived;
import discord.chat.api.interfaces.directmessage.DirectMessageMessageResponse;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.pubsub.RedisClusterPubSubAdapter;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;

@Slf4j
@Component
public class DirectMessageRedisBroker {
    private final StatefulRedisClusterConnection<String, String> redisClusterConnection;
    private final StatefulRedisClusterPubSubConnection<String, String> redisClusterPubSubConnection;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final String directMessageTopic;

    public DirectMessageRedisBroker(
        StatefulRedisClusterConnection<String, String> redisClusterConnection,
        StatefulRedisClusterPubSubConnection<String, String> redisClusterPubSubConnection,
        ThreadPoolTaskExecutor redisMessageExecutor,
        ObjectMapper objectMapper,
        ApplicationEventPublisher applicationEventPublisher,
        @Value("${messaging.redis.direct-message-topic}") String directMessageTopic
    ) {
        this.redisClusterConnection = redisClusterConnection;
        this.redisClusterPubSubConnection = redisClusterPubSubConnection;
        this.objectMapper = objectMapper;
        this.applicationEventPublisher = applicationEventPublisher;
        this.directMessageTopic = directMessageTopic;
        String directMessageChannelPrefix = directMessageTopic + ":";
        redisClusterPubSubConnection.addListener(new RedisClusterPubSubAdapter<>() {
            @Override
            public void smessage(RedisClusterNode redisClusterNode, String channel, String message) {
                if (channel.startsWith(directMessageChannelPrefix)) {
                    String userId = channel.substring(directMessageChannelPrefix.length());
                    redisMessageExecutor.execute(() -> onMessage(userId, message));
                }
            }
        });
    }

    public void subscribe(String userId) {
        try {
            redisClusterPubSubConnection.sync().ssubscribe(directMessageChannel(userId));
        } catch (RuntimeException exception) {
            log.error("Redis direct message subscription failed: userId={}", userId, exception);
        }
    }

    public void unsubscribe(String userId) {
        try {
            redisClusterPubSubConnection.sync().sunsubscribe(directMessageChannel(userId));
        } catch (RuntimeException exception) {
            log.error("Redis direct message unsubscribe failed: userId={}", userId, exception);
        }
    }

    public void publish(DirectMessageMessageResponse message, Collection<String> participantIds) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            log.error("Could not encode direct message: messageId={}", message.messageId(), exception);
            return;
        }

        for (String userId : new LinkedHashSet<>(participantIds)) {
            try {
                redisClusterConnection.sync().spublish(directMessageChannel(userId), payload);
            } catch (RuntimeException exception) {
                log.error(
                    "Redis direct message publication failed: messageId={}, userId={}",
                    message.messageId(), userId, exception
                );
            }
        }
    }

    private void onMessage(String userId, String message) {
        try {
            DirectMessageMessageResponse response = objectMapper.readValue(message, DirectMessageMessageResponse.class);
            applicationEventPublisher.publishEvent(new DirectMessageReceived(userId, response));
        } catch (Exception exception) {
            log.error("Redis direct message delivery failed: userId={}", userId, exception);
        }
    }

    private String directMessageChannel(String userId) {
        return directMessageTopic + ":" + userId;
    }
}
