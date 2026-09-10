package discord.chat.message.test.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import discord.chat.message.application.message.ChatRoomMessageDelivery;
import discord.chat.message.infrastructure.client.chatapi.AccessibleTextChannelResponse;
import discord.chat.message.infrastructure.redis.ChatMessageRedisBroker;
import discord.chat.message.infrastructure.redis.RedisMessagingConfig;
import discord.chat.message.infrastructure.websocket.ChatSessionRegistry;
import discord.chat.message.infrastructure.websocket.WebSocketSessionMessageSender;
import discord.chat.message.interfaces.message.ChatMessageResponse;
import discord.chat.message.interfaces.message.MessageSenderResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.support.TestPropertySourceUtils;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// Requires local Redis. Each context represents a separate message-server instance.
// Only the final WebSocket transport is mocked; Redis and Spring event delivery are real.
class RedisFanOutIntegrationTest {

    // Checks that Redis delivers to both servers in the room, but not another room.
    @Test
    void deliverToBothInstancesInSameRoomOnly() throws Exception {
        String redisTopicPrefix = "discord:test:chat-room:" + UUID.randomUUID();

        try (
            var firstServerContext = createInstance(redisTopicPrefix);
            var secondServerContext = createInstance(redisTopicPrefix);
            var otherRoomServerContext = createInstance(redisTopicPrefix)
        ) {
            registerSession(firstServerContext, "first", "room", "channel");
            registerSession(secondServerContext, "second", "room", "channel");
            registerSession(otherRoomServerContext, "other", "other-room", "other-channel");

            ChatMessageResponse publishedMessage = new ChatMessageResponse(
                "message", "room", "channel",
                new MessageSenderResponse("sender", "Sender", null),
                "hello", Instant.parse("2026-09-07T00:00:00Z")
            );

            firstServerContext.getBean(ChatMessageRedisBroker.class).publish(publishedMessage);

            // Verify real Redis publication, deserialization, and Spring event delivery.
            assertDeliveredMessage(firstServerContext, "first", publishedMessage);
            assertDeliveredMessage(secondServerContext, "second", publishedMessage);

            SimpMessagingTemplate otherRoomMessagingTemplate =
                otherRoomServerContext.getBean(SimpMessagingTemplate.class);
            verify(otherRoomMessagingTemplate, after(500).never()).convertAndSendToUser(
                any(String.class), any(String.class), any(), any(MessageHeaders.class)
            );
        }
    }

    // Starts a separate Spring context for one message server.
    private AnnotationConfigApplicationContext createInstance(String redisTopicPrefix) {
        var serverContext = new AnnotationConfigApplicationContext();
        serverContext.getEnvironment().setActiveProfiles("test");
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            serverContext, "messaging.redis.topic-prefix=" + redisTopicPrefix
        );
        serverContext.register(RedisTestConfig.class);
        try {
            serverContext.refresh();
            return serverContext;
        } catch (RuntimeException exception) {
            serverContext.close();
            throw exception;
        }
    }

    // Gives a session access to a room and starts its Redis subscription.
    private void registerSession(
        AnnotationConfigApplicationContext serverContext,
        String sessionId,
        String chatRoomId,
        String textChannelId
    ) {
        serverContext.getBean(ChatSessionRegistry.class).register(
            sessionId, Set.of(new AccessibleTextChannelResponse(chatRoomId, textChannelId))
        );
    }

    // Waits for delivery and checks the message and target session.
    private void assertDeliveredMessage(
        AnnotationConfigApplicationContext serverContext,
        String sessionId,
        ChatMessageResponse expectedMessage
    ) {
        SimpMessagingTemplate simpMessagingTemplate = serverContext.getBean(SimpMessagingTemplate.class);
        ArgumentCaptor<ChatMessageResponse> deliveredMessageCaptor =
            ArgumentCaptor.forClass(ChatMessageResponse.class);
        ArgumentCaptor<MessageHeaders> messageHeadersCaptor = ArgumentCaptor.forClass(MessageHeaders.class);

        verify(simpMessagingTemplate, timeout(5000)).convertAndSendToUser(
            eq(sessionId), eq("/channel"), deliveredMessageCaptor.capture(), messageHeadersCaptor.capture()
        );

        assertThat(deliveredMessageCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedMessage);
        assertThat(messageHeadersCaptor.getValue().get("simpSessionId")).isEqualTo(sessionId);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @Import({
        RedisMessagingConfig.class,
        ChatMessageRedisBroker.class,
        ChatSessionRegistry.class,
        ChatRoomMessageDelivery.class,
        WebSocketSessionMessageSender.class
    })
    static class RedisTestConfig {
        // Creates a connection factory for the test Redis server.
        @Bean
        LettuceConnectionFactory redisConnectionFactory(
            @Value("${spring.data.redis.host:localhost}") String redisHost,
            @Value("${spring.data.redis.port:6379}") int redisPort
        ) {
            return new LettuceConnectionFactory(redisHost, redisPort);
        }

        // Creates the template used to publish messages to Redis.
        @Bean
        StringRedisTemplate redisTemplate(LettuceConnectionFactory redisConnectionFactory) {
            return new StringRedisTemplate(redisConnectionFactory);
        }

        // Creates the JSON mapper used to write and read messages.
        @Bean
        ObjectMapper objectMapper() {
            return Jackson2ObjectMapperBuilder.json().build();
        }

        // Replaces WebSocket transport with a mock so delivery can be checked.
        @Bean
        SimpMessagingTemplate messagingTemplate() {
            return mock(SimpMessagingTemplate.class);
        }
    }
}
