package discord.chat.message.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import discord.chat.message.interfaces.message.ChatMessageResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class ChatMessageRedisBroker implements MessageListener {
    private final RedisMessageListenerContainer container;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher events;
    private final String topicPrefix;

    public ChatMessageRedisBroker(
        RedisMessageListenerContainer container,
        StringRedisTemplate redisTemplate,
        ObjectMapper objectMapper,
        ApplicationEventPublisher events,
        @Value("${messaging.redis.topic-prefix}") String topicPrefix
    ) {
        this.container = container;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.events = events;
        this.topicPrefix = topicPrefix;
    }

    public void subscribe(String chatRoomId) {
        try {
            // Spring Data Redis waits for the subscription acknowledgment here.
            container.addMessageListener(this, new ChannelTopic(topic(chatRoomId)));
            requireConnection();
        } catch (RuntimeException exception) {
            unsubscribe(chatRoomId);
            throw new IllegalStateException("Could not subscribe to chat room " + chatRoomId, exception);
        }
    }

    public void requireConnection() {
        if (!container.isListening()) {
            throw new IllegalStateException("Redis subscription connection is unavailable");
        }
    }

    public void unsubscribe(String chatRoomId) {
        try {
            container.removeMessageListener(this, new ChannelTopic(topic(chatRoomId)));
        } catch (RuntimeException exception) {
            // The container removes its local mapping before issuing UNSUBSCRIBE.
            log.error("Redis unsubscribe failed: chatRoomId={}", chatRoomId, exception);
        }
    }

    public void publish(ChatMessageResponse message) throws JsonProcessingException {
        Long subscribers = redisTemplate.convertAndSend(
            topic(message.getChatRoomId()), objectMapper.writeValueAsString(message)
        );
        if (subscribers == 0) {
            throw new IllegalStateException("No Redis subscribers for chat room " + message.getChatRoomId());
        }
    }

    @Override
    public void onMessage(@NonNull Message message, @Nullable byte[] pattern) {
        try {
            ChatMessageResponse response = objectMapper.readValue(
                message.getBody(), ChatMessageResponse.class
            );
            String receivedTopic = new String(message.getChannel(), StandardCharsets.UTF_8);
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
