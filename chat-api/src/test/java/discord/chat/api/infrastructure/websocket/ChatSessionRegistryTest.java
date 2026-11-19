package discord.chat.api.infrastructure.websocket;

import discord.chat.api.domain.chat.channel.ChannelAccessService;
import discord.chat.api.infrastructure.redis.ChatMessageRedisBroker;
import discord.chat.api.infrastructure.redis.DirectMessageRedisBroker;
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
        List<String> accessibleRooms = List.of("room");

        when(channelAccessService.getChatRoomIdsBy("user")).thenReturn(accessibleRooms);
        when(channelAccessService.getChatRoomIdsBy("other-user")).thenReturn(accessibleRooms);
        chatSessionRegistry.register("user", "first");
        chatSessionRegistry.register("user", "second");
        chatSessionRegistry.register("other-user", "third");

        assertThat(chatSessionRegistry.hasRoomAccess("user", "room")).isTrue();
        assertThat(chatSessionRegistry.hasRoomAccess("user", "other-room")).isFalse();
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
        List<String> firstRooms = List.of("room");
        List<String> secondRooms = List.of("other");
        when(channelAccessService.getChatRoomIdsBy("first-user")).thenReturn(firstRooms);
        when(channelAccessService.getChatRoomIdsBy("second-user")).thenReturn(secondRooms);
        chatSessionRegistry.register("first-user", "same-room");
        chatSessionRegistry.register("second-user", "other-room");

        assertThat(chatSessionRegistry.getUserIds("room")).containsExactly("first-user");
        assertThat(chatSessionRegistry.getUserIds("other")).containsExactly("second-user");
    }

    // Checks that a failed new registration keeps the existing session.
    @Test
    void keepExistingSessionOnRegistrationFailure() {
        List<String> accessibleRooms = List.of("room");

        when(channelAccessService.getChatRoomIdsBy("existing-user")).thenReturn(accessibleRooms);
        when(channelAccessService.getChatRoomIdsBy("new-user")).thenReturn(accessibleRooms);
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
        when(channelAccessService.hasChatRoomAccess("user", "room")).thenReturn(true);

        chatSessionRegistry.join("user", "room");

        assertThat(chatSessionRegistry.hasRoomAccess("user", "room")).isTrue();

        chatSessionRegistry.leave("user", "room");

        assertThat(chatSessionRegistry.hasRoomAccess("user", "room")).isFalse();
        verify(chatMessageRedisBroker).unsubscribe("room");
    }

    // Checks that deleting a room removes it from every local user.
    @Test
    void removeDeletedRoomFromEveryUser() {
        List<String> accessibleRooms = List.of("room");
        when(channelAccessService.getChatRoomIdsBy("first-user")).thenReturn(accessibleRooms);
        when(channelAccessService.getChatRoomIdsBy("second-user")).thenReturn(accessibleRooms);
        chatSessionRegistry.register("first-user", "first");
        chatSessionRegistry.register("second-user", "second");

        chatSessionRegistry.delete("room");

        assertThat(chatSessionRegistry.getUserIds("room")).isEmpty();
        assertThat(chatSessionRegistry.hasRoomAccess("first-user", "room")).isFalse();
        assertThat(chatSessionRegistry.hasRoomAccess("second-user", "room")).isFalse();
        verify(chatMessageRedisBroker).unsubscribe("room");
    }

    // Checks that a join skips the database when the user is not connected locally.
    @Test
    void ignoreJoinForDisconnectedUser() {
        chatSessionRegistry.join("user", "room");

        verifyNoInteractions(channelAccessService, chatMessageRedisBroker);
    }

}
