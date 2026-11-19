package discord.chat.api.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import discord.chat.api.application.session.ChatSessionEvent;
import discord.chat.api.interfaces.message.ChatMessageResponse;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.pubsub.RedisClusterPubSubAdapter;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageRedisBroker {
    private final StatefulRedisClusterConnection<String, String> redisClusterConnection;
    private final StatefulRedisClusterPubSubConnection<String, String> redisClusterPubSubConnection;
    private final ObjectMapper objectMapper;
    private final RedisChannels redisChannels;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ThreadPoolTaskExecutor redisMessageExecutor;

    @PostConstruct
    void registerListener() {
        redisClusterPubSubConnection.addListener(new RedisClusterPubSubAdapter<>() {
            @Override
            public void smessage(RedisClusterNode node, String channel, String message) {
                if (redisChannels.isRoom(channel)) {
                    redisMessageExecutor.execute(() -> onMessage(channel, message));
                }
            }
        });
    }

    public void subscribe(String chatRoomId) {
        try {
            redisClusterPubSubConnection.sync().ssubscribe(redisChannels.room(chatRoomId));
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
            redisClusterPubSubConnection.sync().sunsubscribe(redisChannels.room(chatRoomId));
        } catch (RuntimeException exception) {
            log.error("Redis shard unsubscribe failed: chatRoomId={}", chatRoomId, exception);
        }
    }

    public void publish(ChatMessageResponse message) throws JsonProcessingException {
        String payload = objectMapper.writeValueAsString(
            new OutgoingRedisPayload(RedisPayloadType.MESSAGE, message)
        );
        long subscribers = redisClusterConnection.sync().spublish(
            redisChannels.room(message.getChatRoomId()), payload
        );
        if (subscribers == 0) {
            throw new IllegalStateException("No Redis subscribers for chat room " + message.getChatRoomId());
        }
    }

    private void onMessage(String channel, String message) {
        try {
            var payload = objectMapper.readValue(message, IncomingRedisPayload.class);
            switch (payload.type()) {
                case MESSAGE -> {
                    ChatMessageResponse response = objectMapper.treeToValue(payload.data(), ChatMessageResponse.class);
                    requireRoomChannel(channel, response.getChatRoomId());
                    applicationEventPublisher.publishEvent(response);
                }
                case SESSION_EVENT -> {
                    ChatSessionEvent event = objectMapper.treeToValue(payload.data(), ChatSessionEvent.class);
                    requireRoomChannel(channel, event.chatRoomId());
                    applicationEventPublisher.publishEvent(event);
                }
            }
        } catch (Exception exception) {
            log.error("Redis room delivery failed: channel={}", channel, exception);
        }
    }

    private void requireRoomChannel(String channel, String chatRoomId) {
        if (!channel.equals(redisChannels.room(chatRoomId))) {
            throw new IllegalArgumentException("Redis channel does not match chat room " + chatRoomId);
        }
    }
}
