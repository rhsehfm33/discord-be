package discord.chat.api.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import discord.chat.api.application.message.DirectMessageDelivery.DirectMessageReceived;
import discord.chat.api.application.session.ChatSessionEvent;
import discord.chat.api.application.session.ChatSessionEventHandler.PersonalJoinReceived;
import discord.chat.api.interfaces.directmessage.DirectMessageMessageResponse;
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

import java.util.Collection;
import java.util.LinkedHashSet;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectMessageRedisBroker {
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
                if (redisChannels.isPersonal(channel)) {
                    String userId = redisChannels.personalUserId(channel);
                    redisMessageExecutor.execute(() -> onMessage(userId, message));
                }
            }
        });
    }

    public void subscribe(String userId) {
        try {
            redisClusterPubSubConnection.sync().ssubscribe(redisChannels.personal(userId));
        } catch (RuntimeException exception) {
            log.error("Redis direct message subscription failed: userId={}", userId, exception);
        }
    }

    public void unsubscribe(String userId) {
        try {
            redisClusterPubSubConnection.sync().sunsubscribe(redisChannels.personal(userId));
        } catch (RuntimeException exception) {
            log.error("Redis direct message unsubscribe failed: userId={}", userId, exception);
        }
    }

    public void publish(DirectMessageMessageResponse message, Collection<String> participantIds) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(
                new OutgoingRedisPayload(RedisPayloadType.MESSAGE, message)
            );
        } catch (JsonProcessingException exception) {
            log.error("Could not encode direct message: messageId={}", message.messageId(), exception);
            return;
        }

        for (String userId : new LinkedHashSet<>(participantIds)) {
            try {
                redisClusterConnection.sync().spublish(redisChannels.personal(userId), payload);
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
            var payload = objectMapper.readValue(message, IncomingRedisPayload.class);
            switch (payload.type()) {
                case MESSAGE -> {
                    DirectMessageMessageResponse response = objectMapper.treeToValue(
                        payload.data(), DirectMessageMessageResponse.class
                    );
                    applicationEventPublisher.publishEvent(new DirectMessageReceived(userId, response));
                }
                case SESSION_EVENT -> {
                    ChatSessionEvent event = objectMapper.treeToValue(payload.data(), ChatSessionEvent.class);
                    if (event.type() != ChatSessionEvent.Type.JOIN || !userId.equals(event.userId())) {
                        throw new IllegalArgumentException("Expected JOIN for the subscribed user");
                    }
                    applicationEventPublisher.publishEvent(new PersonalJoinReceived(userId, event.chatRoomId()));
                }
            }
        } catch (Exception exception) {
            log.error("Redis personal channel delivery failed: userId={}", userId, exception);
        }
    }
}
