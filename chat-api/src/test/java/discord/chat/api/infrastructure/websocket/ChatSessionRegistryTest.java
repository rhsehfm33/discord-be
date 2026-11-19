package discord.chat.api.infrastructure.websocket;

import discord.chat.api.domain.chat.channel.ChannelAccessService;
import discord.chat.api.infrastructure.redis.ChatMessageRedisBroker;
import discord.chat.api.infrastructure.redis.DirectMessageRedisBroker;
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
    @Mock
    private DirectMessageRedisBroker directMessageRedisBroker;
    @Mock
    private ChannelAccessService channelAccessService;

    private ChatSessionRegistry chatSessionRegistry;

    // Creates the session registry with mocked Redis brokers and channel access.
    @BeforeEach
    void setUp() {
        chatSessionRegistry = new ChatSessionRegistry(
            chatMessageRedisBroker, directMessageRedisBroker, channelAccessService
        );
    }

    // Checks that Redis remains subscribed until the room's last local user disconnects.
    @Test
    void manageRoomSubscription() {
        List<TextChannel> accessibleTextChannels = createAccessibleChannels("room", "channel");

        when(channelAccessService.getTextChannelsBy("user")).thenReturn(accessibleTextChannels);
        when(channelAccessService.getTextChannelsBy("other-user")).thenReturn(accessibleTextChannels);
        chatSessionRegistry.register("user", "first");
        chatSessionRegistry.register("user", "second");
        chatSessionRegistry.register("other-user", "third");

        assertThat(chatSessionRegistry.hasChannelAccess("user", "room", "channel")).isTrue();
        assertThat(chatSessionRegistry.hasChannelAccess("user", "other-room", "channel")).isFalse();
        verify(chatMessageRedisBroker).subscribe("room");
        verify(chatMessageRedisBroker).requireConnection();

        chatSessionRegistry.removeSession("user", "first");
        verify(chatMessageRedisBroker, never()).unsubscribe("room");

        chatSessionRegistry.removeSession("user", "second");
        verify(chatMessageRedisBroker, never()).unsubscribe("room");

        chatSessionRegistry.removeSession("other-user", "third");
        verify(chatMessageRedisBroker).unsubscribe("room");
    }

    // Checks that each room returns only its own users.
    @Test
    void getUserIdsForRequestedRoomOnly() {
        List<TextChannel> firstChannels = createAccessibleChannels("room", "channel");
        List<TextChannel> secondChannels = createAccessibleChannels("other", "other-channel");
        when(channelAccessService.getTextChannelsBy("first-user")).thenReturn(firstChannels);
        when(channelAccessService.getTextChannelsBy("second-user")).thenReturn(secondChannels);
        chatSessionRegistry.register("first-user", "same-room");
        chatSessionRegistry.register("second-user", "other-room");

        assertThat(chatSessionRegistry.getUserIds("room")).containsExactly("first-user");
        assertThat(chatSessionRegistry.getUserIds("other")).containsExactly("second-user");
    }

    // Checks that a failed new registration keeps the existing session.
    @Test
    void keepExistingSessionOnRegistrationFailure() {
        List<TextChannel> accessibleTextChannels = createAccessibleChannels("room", "channel");

        when(channelAccessService.getTextChannelsBy("existing-user")).thenReturn(accessibleTextChannels);
        when(channelAccessService.getTextChannelsBy("new-user")).thenReturn(accessibleTextChannels);
        chatSessionRegistry.register("existing-user", "existing");
        doThrow(new IllegalStateException("Redis unavailable"))
            .when(chatMessageRedisBroker)
            .requireConnection();

        assertThatThrownBy(() -> chatSessionRegistry.register("new-user", "new"))
            .isInstanceOf(IllegalStateException.class);
        assertThat(chatSessionRegistry.getUserIds("room")).containsExactly("existing-user");
    }

    // Checks that room access is shared by every session owned by a user.
    @Test
    void updateEverySessionForUserMembership() {
        chatSessionRegistry.register("user", "first");
        chatSessionRegistry.register("user", "second");
        TextChannel textChannel = mock(TextChannel.class);
        when(textChannel.getId()).thenReturn("channel");
        when(channelAccessService.getTextChannelsBy("user", "room")).thenReturn(List.of(textChannel));

        chatSessionRegistry.join("user", "room");

        assertThat(chatSessionRegistry.hasChannelAccess("user", "room", "channel")).isTrue();

        chatSessionRegistry.leave("user", "room");

        assertThat(chatSessionRegistry.hasChannelAccess("user", "room", "channel")).isFalse();
        verify(chatMessageRedisBroker).unsubscribe("room");
    }

    // Checks that deleting a room removes it from every local user.
    @Test
    void removeDeletedRoomFromEveryUser() {
        List<TextChannel> accessibleTextChannels = createAccessibleChannels("room", "channel");
        when(channelAccessService.getTextChannelsBy("first-user")).thenReturn(accessibleTextChannels);
        when(channelAccessService.getTextChannelsBy("second-user")).thenReturn(accessibleTextChannels);
        chatSessionRegistry.register("first-user", "first");
        chatSessionRegistry.register("second-user", "second");

        chatSessionRegistry.delete("room");

        assertThat(chatSessionRegistry.getUserIds("room")).isEmpty();
        assertThat(chatSessionRegistry.hasChannelAccess("first-user", "room", "channel")).isFalse();
        assertThat(chatSessionRegistry.hasChannelAccess("second-user", "room", "channel")).isFalse();
        verify(chatMessageRedisBroker).unsubscribe("room");
    }

    // Checks that a join skips the database when the user is not connected locally.
    @Test
    void ignoreJoinForDisconnectedUser() {
        chatSessionRegistry.join("user", "room");

        verifyNoInteractions(channelAccessService, chatMessageRedisBroker);
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
