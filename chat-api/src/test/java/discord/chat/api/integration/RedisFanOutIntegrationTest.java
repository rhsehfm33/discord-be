package discord.chat.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import discord.chat.api.application.message.ChatRoomMessageDelivery;
import discord.chat.api.application.session.ChatSessionEvent;
import discord.chat.api.application.session.ChatSessionEventHandler;
import discord.chat.api.domain.chat.channel.ChannelAccessService;
import discord.chat.api.infrastructure.redis.ChatMessageRedisBroker;
import discord.chat.api.infrastructure.redis.ChatRoomRedisBroker;
import discord.chat.api.infrastructure.redis.RedisMessagingConfig;
import discord.chat.api.infrastructure.websocket.ChatSessionRegistry;
import discord.chat.api.infrastructure.websocket.WebSocketUserMessageSender;
import discord.chat.api.interfaces.message.ChatMessageResponse;
import discord.chat.api.interfaces.message.MessageSenderResponse;
import discord.chat.common.infrastructure.chat.channel.TextChannel;
import discord.chat.common.infrastructure.chat.room.ChatRoom;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.support.TestPropertySourceUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// Requires local Redis. Each context represents a separate message-server instance.
// Only the final WebSocket transport is mocked; Redis and Spring event delivery are real.
class RedisFanOutIntegrationTest {

    // Checks that a membership event updates one user's sessions across server instances.
    @Test
    void updateUserSessionsAcrossInstances() throws Exception {
        String redisTopicPrefix = "discord:test:chat-room:" + UUID.randomUUID();

        try (
            var firstServerContext = createInstance(redisTopicPrefix);
            var secondServerContext = createInstance(redisTopicPrefix)
        ) {
            registerEmptySession(firstServerContext, "user", "first");
            registerEmptySession(secondServerContext, "user", "second");
            allowRoomAccess(firstServerContext, "user", "room", "channel");
            allowRoomAccess(secondServerContext, "user", "room", "channel");

            firstServerContext.getBean(ChatRoomRedisBroker.class)
                .publish(ChatSessionEvent.join("user", "nickname", "room"));

            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                assertThat(firstServerContext.getBean(ChatSessionRegistry.class)
                    .hasChannelAccess("user", "room", "channel")).isTrue();
                assertThat(secondServerContext.getBean(ChatSessionRegistry.class)
                    .hasChannelAccess("user", "room", "channel")).isTrue();
            });
        }
    }

    // Checks that Redis delivers to both servers in the room, but not another room.
    @Test
    void deliverToBothInstancesInSameRoomOnly() throws Exception {
        String redisTopicPrefix = "discord:test:chat-room:" + UUID.randomUUID();

        try (
            var firstServerContext = createInstance(redisTopicPrefix);
            var secondServerContext = createInstance(redisTopicPrefix);
            var otherRoomServerContext = createInstance(redisTopicPrefix)
        ) {
            registerSession(firstServerContext, "first-user", "first", "room", "channel");
            registerSession(secondServerContext, "second-user", "second", "room", "channel");
            registerSession(otherRoomServerContext, "other-user", "other", "other-room", "other-channel");

            ChatMessageResponse publishedMessage = new ChatMessageResponse(
                "message", "room", "channel",
                new MessageSenderResponse("sender", "Sender", null),
                "hello", Instant.parse("2026-09-07T00:00:00Z")
            );

            firstServerContext.getBean(ChatMessageRedisBroker.class).publish(publishedMessage);

            // Verify real Redis publication, deserialization, and Spring event delivery.
            assertDeliveredMessage(firstServerContext, "first-user", publishedMessage);
            assertDeliveredMessage(secondServerContext, "second-user", publishedMessage);

            SimpMessagingTemplate otherRoomMessagingTemplate =
                otherRoomServerContext.getBean(SimpMessagingTemplate.class);
            verify(otherRoomMessagingTemplate, after(500).never()).convertAndSendToUser(
                any(String.class), any(String.class), any()
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
        String userId,
        String sessionId,
        String chatRoomId,
        String textChannelId
    ) {
        TextChannel textChannel = mock(TextChannel.class);
        ChatRoom chatRoom = mock(ChatRoom.class);
        when(textChannel.getId()).thenReturn(textChannelId);
        when(textChannel.getChatRoom()).thenReturn(chatRoom);
        when(chatRoom.getId()).thenReturn(chatRoomId);
        serverContext.getBean(ChatSessionRegistry.class).register(
            userId, sessionId, List.of(textChannel)
        );
    }

    // Registers a connected user before the user has access to a room.
    private void registerEmptySession(
        AnnotationConfigApplicationContext serverContext,
        String userId,
        String sessionId
    ) {
        serverContext.getBean(ChatSessionRegistry.class).register(userId, sessionId, List.of());
    }

    // Gives the event handler current database access for one room.
    private void allowRoomAccess(
        AnnotationConfigApplicationContext serverContext,
        String userId,
        String chatRoomId,
        String textChannelId
    ) {
        TextChannel textChannel = mock(TextChannel.class);
        ChatRoom chatRoom = mock(ChatRoom.class);
        when(textChannel.getId()).thenReturn(textChannelId);
        when(textChannel.getChatRoom()).thenReturn(chatRoom);
        when(chatRoom.getId()).thenReturn(chatRoomId);
        ChannelAccessService channelAccessService = serverContext.getBean(ChannelAccessService.class);
        when(channelAccessService.getTextChannelsBy(userId, chatRoomId)).thenReturn(List.of(textChannel));
    }

    // Waits for delivery and checks the message and target user.
    private void assertDeliveredMessage(
        AnnotationConfigApplicationContext serverContext,
        String userId,
        ChatMessageResponse expectedMessage
    ) {
        SimpMessagingTemplate simpMessagingTemplate = serverContext.getBean(SimpMessagingTemplate.class);
        ArgumentCaptor<ChatMessageResponse> deliveredMessageCaptor =
            ArgumentCaptor.forClass(ChatMessageResponse.class);
        verify(simpMessagingTemplate, timeout(5000)).convertAndSendToUser(
            eq(userId), eq("/channel"), deliveredMessageCaptor.capture()
        );

        assertThat(deliveredMessageCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedMessage);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @Import({
        RedisMessagingConfig.class,
        ChatMessageRedisBroker.class,
        ChatRoomRedisBroker.class,
        ChatSessionEventHandler.class,
        ChatSessionRegistry.class,
        ChatRoomMessageDelivery.class,
        WebSocketUserMessageSender.class
    })
    static class RedisTestConfig {
        // Creates a cluster-aware connection factory for the local Redis nodes.
        @Bean
        LettuceConnectionFactory redisConnectionFactory() {
            RedisClusterConfiguration redisClusterConfiguration = new RedisClusterConfiguration(
                List.of("localhost:7000", "localhost:7001", "localhost:7002")
            );
            return new LettuceConnectionFactory(redisClusterConfiguration);
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

        // Replaces database access with a mock for session membership events.
        @Bean
        ChannelAccessService channelAccessService() {
            return mock(ChannelAccessService.class);
        }
    }
}
