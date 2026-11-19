package discord.chat.api.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import discord.chat.api.application.session.ChatSessionEvent;
import discord.chat.api.application.session.TextChannelEvent;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSessionEventPublisher {
    private final StatefulRedisClusterConnection<String, String> redisClusterConnection;
    private final RedisChannels redisChannels;
    private final ObjectMapper objectMapper;

    public void publishToUserAfterCommit(ChatSessionEvent event) {
        publishAfterCommit(redisChannels.personal(event.userId()), new OutgoingRedisPayload(RedisPayloadType.SESSION_EVENT, event));
    }

    public void publishToRoomAfterCommit(ChatSessionEvent event) {
        publishAfterCommit(redisChannels.room(event.chatRoomId()), new OutgoingRedisPayload(RedisPayloadType.SESSION_EVENT, event));
    }

    public void publishToRoomAfterCommit(TextChannelEvent event) {
        publishAfterCommit(redisChannels.room(event.chatRoomId()), new OutgoingRedisPayload(RedisPayloadType.TEXT_CHANNEL_EVENT, event));
    }

    private void publishAfterCommit(String channel, OutgoingRedisPayload event) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
            || !TransactionSynchronizationManager.isSynchronizationActive()) {
            publish(channel, event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish(channel, event);
            }
        });
    }

    private void publish(String channel, OutgoingRedisPayload event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            RedisFuture<Long> publication = redisClusterConnection.async().spublish(channel, payload);
            publication.whenComplete((subscriberCount, error) -> {
                if (error != null) {
                    logPublicationFailure(channel, event, error);
                }
            });
        } catch (Exception exception) {
            logPublicationFailure(channel, event, exception);
        }
    }

    private void logPublicationFailure(String channel, OutgoingRedisPayload event, Throwable error) {
        log.error("Redis event publication failed: channel={}, event={}", channel, event.data(), error);
    }
}
