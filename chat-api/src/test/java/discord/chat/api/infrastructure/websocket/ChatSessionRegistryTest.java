package discord.chat.api.infrastructure.websocket;

import discord.chat.api.infrastructure.redis.ChatMessageRedisBroker;
import discord.chat.common.infrastructure.chat.channel.TextChannel;
import discord.chat.common.infrastructure.chat.room.ChatRoom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatSessionRegistryTest {
    @Mock
    private ChatMessageRedisBroker chatMessageRedisBroker;

    private ChatSessionRegistry chatSessionRegistry;

    // Creates the session registry with a mocked Redis broker.
    @BeforeEach
    void setUp() {
        chatSessionRegistry = new ChatSessionRegistry(chatMessageRedisBroker);
    }

    // Checks that the first session subscribes and the last session unsubscribes.
    @Test
    void manageRoomSubscription() {
        List<TextChannel> accessibleTextChannels = createAccessibleChannels("room", "channel");

        chatSessionRegistry.register("first", accessibleTextChannels);
        chatSessionRegistry.register("second", accessibleTextChannels);

        assertThat(chatSessionRegistry.hasChannelAccess("first", "room", "channel")).isTrue();
        assertThat(chatSessionRegistry.hasChannelAccess("first", "other-room", "channel")).isFalse();
        verify(chatMessageRedisBroker).subscribe("room");
        verify(chatMessageRedisBroker).requireConnection();

        chatSessionRegistry.removeSession("first");
        verify(chatMessageRedisBroker, never()).unsubscribe("room");

        chatSessionRegistry.removeSession("second");
        verify(chatMessageRedisBroker).unsubscribe("room");
    }

    // Checks that each room returns only its own sessions.
    @Test
    void getSessionIdsForRequestedRoomOnly() {
        chatSessionRegistry.register("same-room", createAccessibleChannels("room", "channel"));
        chatSessionRegistry.register("other-room", createAccessibleChannels("other", "other-channel"));

        assertThat(chatSessionRegistry.getSessionIds("room")).containsExactly("same-room");
        assertThat(chatSessionRegistry.getSessionIds("other")).containsExactly("other-room");
    }

    // Checks that a failed new registration keeps the existing session.
    @Test
    void keepExistingSessionOnRegistrationFailure() {
        List<TextChannel> accessibleTextChannels = createAccessibleChannels("room", "channel");

        chatSessionRegistry.register("existing", accessibleTextChannels);
        doThrow(new IllegalStateException("Redis unavailable"))
            .when(chatMessageRedisBroker)
            .requireConnection();

        assertThatThrownBy(() -> chatSessionRegistry.register("new", accessibleTextChannels))
            .isInstanceOf(IllegalStateException.class);
        assertThat(chatSessionRegistry.getSessionIds("room")).containsExactly("existing");
    }

    // Creates channel access for the given room.
    private List<TextChannel> createAccessibleChannels(
        String chatRoomId,
        String textChannelId
    ) {
        TextChannel textChannel = mock(TextChannel.class);
        ChatRoom chatRoom = mock(ChatRoom.class);
        when(textChannel.getId()).thenReturn(textChannelId);
        when(textChannel.getChatRoom()).thenReturn(chatRoom);
        when(chatRoom.getId()).thenReturn(chatRoomId);
        return List.of(textChannel);
    }
}
