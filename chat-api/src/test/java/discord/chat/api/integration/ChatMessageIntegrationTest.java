package discord.chat.api.integration;

import discord.chat.api.application.message.ChatMessageService;
import discord.chat.api.infrastructure.message.ChatMessageRepository;
import discord.chat.api.infrastructure.redis.ChatMessageRedisBroker;
import discord.chat.api.infrastructure.websocket.ChatSessionRegistry;
import discord.chat.api.infrastructure.websocket.WebSocketUserMessageSender;
import discord.chat.api.support.BaseIntegrationTest;
import discord.chat.common.infrastructure.chat.channel.TextChannel;
import discord.chat.common.infrastructure.chat.room.ChatRoom;
import discord.chat.common.infrastructure.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Uses real MongoDB persistence; outbound delivery is outside this test's scope.
class ChatMessageIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private ChatSessionRegistry chatSessionRegistry;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @MockBean
    private ChatMessageRedisBroker chatMessageRedisBroker;

    @MockBean
    private WebSocketUserMessageSender webSocketUserMessageSender;

    // Gives the test session access to the channel.
    @BeforeEach
    void registerSession() {
        TextChannel textChannel = mock(TextChannel.class);
        ChatRoom chatRoom = mock(ChatRoom.class);
        when(textChannel.getId()).thenReturn("channel");
        when(textChannel.getChatRoom()).thenReturn(chatRoom);
        when(chatRoom.getId()).thenReturn("room");
        chatSessionRegistry.register("sender", "session", List.of(textChannel));
    }

    // Removes the test session after each test.
    @AfterEach
    void removeSession() {
        chatSessionRegistry.removeSession("sender", "session");
    }

    // Checks that the message is saved in MongoDB.
    @Test
    void storeMessage() {
        sendMessage();

        assertThat(chatMessageRepository.findAll()).singleElement().satisfies(storedMessage -> {
            assertThat(storedMessage.getId()).isNotBlank();
            assertThat(storedMessage.getSenderId()).isEqualTo("sender");
            assertThat(storedMessage.getChatRoomId()).isEqualTo("room");
            assertThat(storedMessage.getTextChannelId()).isEqualTo("channel");
            assertThat(storedMessage.getContent()).isEqualTo("hello");
            assertThat(storedMessage.getCreatedAt()).isNotNull();
        });
    }

    // Checks that the saved message remains when Redis publication fails.
    @Test
    void keepStoredMessageWhenPublicationFails() throws Exception {
        doThrow(new IllegalStateException("Redis unavailable")).when(chatMessageRedisBroker).publish(any());

        sendMessage();

        assertThat(chatMessageRepository.findAll()).singleElement().satisfies(storedMessage -> {
            assertThat(storedMessage.getId()).isNotBlank();
            assertThat(storedMessage.getContent()).isEqualTo("hello");
        });
    }

    // Sends a sample message as an authenticated user.
    private void sendMessage() {
        User sendingUser = new User("sender", "Sender", "sender@example.com", null, "image.png");
        var senderAuthentication = new UsernamePasswordAuthenticationToken(sendingUser, null, List.of());
        chatMessageService.send("room", "channel", "hello", senderAuthentication);
    }
}
