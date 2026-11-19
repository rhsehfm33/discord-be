package discord.chat.api.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import discord.chat.api.application.session.ChatSessionEvent;
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
        publishAfterCommit(event, redisChannels.personal(event.userId()));
    }

    public void publishToRoomAfterCommit(ChatSessionEvent event) {
        publishAfterCommit(event, redisChannels.room(event.chatRoomId()));
    }

    private void publishAfterCommit(ChatSessionEvent event, String channel) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
            || !TransactionSynchronizationManager.isSynchronizationActive()) {
            publish(event, channel);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
            publish(event, channel);
            }
        });
    }

    private void publish(ChatSessionEvent event, String channel) {
        try {
            String payload = objectMapper.writeValueAsString(
                new OutgoingRedisPayload(RedisPayloadType.SESSION_EVENT, event)
            );
            RedisFuture<Long> publication = redisClusterConnection.async().spublish(channel, payload);
            publication.whenComplete((subscriberCount, error) -> {
                if (error != null) {
                    logPublicationFailure(event, error);
                }
            });
        } catch (Exception exception) {
            logPublicationFailure(event, exception);
        }
    }

    private void logPublicationFailure(ChatSessionEvent event, Throwable error) {
        log.error("Chat session event publication failed: type={}, userId={}, chatRoomId={}",
            event.type(), event.userId(), event.chatRoomId(), error);
    }
}
