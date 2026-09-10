package discord.chat.message.test.unit;

import discord.chat.message.application.message.ChatRoomMessageDelivery;
import discord.chat.message.infrastructure.client.chatapi.AccessibleTextChannelResponse;
import discord.chat.message.infrastructure.redis.ChatMessageRedisBroker;
import discord.chat.message.infrastructure.websocket.ChatSessionRegistry;
import discord.chat.message.infrastructure.websocket.WebSocketSessionMessageSender;
import discord.chat.message.interfaces.message.MessageSenderResponse;
import discord.chat.message.interfaces.message.ChatMessageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisFanOutTest {
    @Mock
    private ChatMessageRedisBroker chatMessageRedisBroker;

    @Mock
    private WebSocketSessionMessageSender webSocketSessionMessageSender;

    private ChatSessionRegistry chatSessionRegistry;
    private ChatRoomMessageDelivery chatRoomMessageDelivery;

    // Creates the session registry and delivery service with mocked dependencies.
    @BeforeEach
    void setUp() {
        chatSessionRegistry = new ChatSessionRegistry(chatMessageRedisBroker);
        chatRoomMessageDelivery = new ChatRoomMessageDelivery(chatSessionRegistry, webSocketSessionMessageSender);
    }

    // Checks that the first session subscribes and the last session unsubscribes.
    @Test
    void manageRoomSubscription() {
        Set<AccessibleTextChannelResponse> accessibleTextChannels = createAccessibleChannels("room", "channel");

        chatSessionRegistry.register("first", accessibleTextChannels);
        chatSessionRegistry.register("second", accessibleTextChannels);

        verify(chatMessageRedisBroker).subscribe("room");
        verify(chatMessageRedisBroker).requireConnection();

        chatSessionRegistry.remove("first");
        verify(chatMessageRedisBroker, never()).unsubscribe("room");

        chatSessionRegistry.remove("second");
        verify(chatMessageRedisBroker).unsubscribe("room");
    }

    // Checks that only sessions in the message's room receive it.
    @Test
    void deliverMessageToRoomSessions() {
        chatSessionRegistry.register("same-room", createAccessibleChannels("room", "channel"));
        chatSessionRegistry.register("other-room", createAccessibleChannels("other", "other-channel"));

        ChatMessageResponse chatMessageResponse = new ChatMessageResponse(
            "message",
            "room",
            "channel",
            new MessageSenderResponse("sender", "Sender", null),
            "hello",
            Instant.parse("2026-09-07T00:00:00Z")
        );
        chatRoomMessageDelivery.deliver(chatMessageResponse);

        verify(webSocketSessionMessageSender).send("same-room", "/channel", chatMessageResponse);
        verify(webSocketSessionMessageSender, never()).send("other-room", "/channel", chatMessageResponse);
    }

    // Checks that a failed new registration keeps the existing session.
    @Test
    void keepExistingSessionOnRegistrationFailure() {
        Set<AccessibleTextChannelResponse> accessibleTextChannels = createAccessibleChannels("room", "channel");

        chatSessionRegistry.register("existing", accessibleTextChannels);
        doThrow(new IllegalStateException("Redis unavailable"))
            .when(chatMessageRedisBroker)
            .requireConnection();

        assertThatThrownBy(() -> chatSessionRegistry.register("new", accessibleTextChannels))
            .isInstanceOf(IllegalStateException.class);
        assertThat(chatSessionRegistry.getSessionIds("room")).containsExactly("existing");
    }

    // Creates channel access for the given room.
    private Set<AccessibleTextChannelResponse> createAccessibleChannels(
        String chatRoomId,
        String textChannelId
    ) {
        return Set.of(new AccessibleTextChannelResponse(chatRoomId, textChannelId));
    }
}
