package discord.chat.api.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import discord.chat.api.application.session.ChatSessionEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class ChatRoomRedisBroker implements MessageListener {
    private static final String TOPIC_SUFFIX = ":session-events";

    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ChannelTopic channelTopic;

    public ChatRoomRedisBroker(
        RedisMessageListenerContainer redisMessageListenerContainer,
        StringRedisTemplate stringRedisTemplate,
        ObjectMapper objectMapper,
        ApplicationEventPublisher applicationEventPublisher,
        @Value("${messaging.redis.topic-prefix}") String topicPrefix
    ) {
        this.redisMessageListenerContainer = redisMessageListenerContainer;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.applicationEventPublisher = applicationEventPublisher;
        this.channelTopic = new ChannelTopic(topicPrefix + TOPIC_SUFFIX);
    }

    @PostConstruct
    public void subscribe() {
        redisMessageListenerContainer.addMessageListener(this, channelTopic);
    }

    @PreDestroy
    public void unsubscribe() {
        redisMessageListenerContainer.removeMessageListener(this, channelTopic);
    }

    public void publish(ChatSessionEvent event) throws JsonProcessingException {
        Long subscriberCount = stringRedisTemplate.convertAndSend(
            channelTopic.getTopic(),
            objectMapper.writeValueAsString(event)
        );
        if (subscriberCount == 0) {
            throw new IllegalStateException("No Redis subscribers for chat session events");
        }
    }

    @Override
    public void onMessage(@NonNull Message message, @Nullable byte[] pattern) {
        try {
            String receivedTopic = new String(message.getChannel(), StandardCharsets.UTF_8);
            if (!channelTopic.getTopic().equals(receivedTopic)) {
                throw new IllegalArgumentException("Unexpected Redis chat session topic");
            }

            ChatSessionEvent event = objectMapper.readValue(message.getBody(), ChatSessionEvent.class);
            applicationEventPublisher.publishEvent(event);
        } catch (Exception exception) {
            log.error("Redis chat session event handling failed", exception);
        }
    }
}
