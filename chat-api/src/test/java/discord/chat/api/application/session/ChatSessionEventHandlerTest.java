package discord.chat.api.application.session;

import discord.chat.api.domain.chat.channel.ChannelAccessService;
import discord.chat.api.infrastructure.websocket.ChatSessionRegistry;
import discord.chat.api.infrastructure.websocket.WebSocketUserMessageSender;
import discord.chat.api.interfaces.chat.room.ChatRoomEventResponse;
import discord.chat.common.infrastructure.chat.channel.TextChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatSessionEventHandlerTest {
    @Mock
    private ChannelAccessService channelAccessService;
    @Mock
    private ChatSessionRegistry chatSessionRegistry;
    @Mock
    private WebSocketUserMessageSender webSocketUserMessageSender;

    private ChatSessionEventHandler chatSessionEventHandler;

    // Creates the event handler with mocked dependencies.
    @BeforeEach
    void setUp() {
        chatSessionEventHandler = new ChatSessionEventHandler(
            channelAccessService,
            chatSessionRegistry,
            webSocketUserMessageSender
        );
    }

    // Checks that a join event loads channels belonging to the joined room.
    @Test
    void joinAccessibleRoom() {
        TextChannel joinedRoomChannel = createTextChannel();
        when(chatSessionRegistry.hasUserSessions("user")).thenReturn(true);
        when(channelAccessService.getTextChannelsBy("user", "joined-room"))
            .thenReturn(List.of(joinedRoomChannel));

        chatSessionEventHandler.handle(ChatSessionEvent.join("user", "nickname", "joined-room"));

        verify(chatSessionRegistry).join("user", List.of(joinedRoomChannel));
    }

    // Checks that a join event avoids a database lookup when the user is not connected locally.
    @Test
    void ignoreJoinForDisconnectedUser() {
        when(chatSessionRegistry.hasUserSessions("user")).thenReturn(false);

        chatSessionEventHandler.handle(ChatSessionEvent.join("user", "nickname", "room"));

        verify(channelAccessService, never()).getTextChannelsBy("user", "room");
        verify(chatSessionRegistry, never()).join(anyString(), anyList());
    }

    // Checks that a leave event removes the user's cached room access.
    @Test
    void leaveRoom() {
        chatSessionEventHandler.handle(ChatSessionEvent.leave("user", "nickname", "room"));

        verify(chatSessionRegistry).leave("user", "room");
    }

    // Checks that a delete event notifies local participants before removing cached room access.
    @Test
    void deleteRoom() {
        LinkedHashSet<String> userIds = new LinkedHashSet<>(List.of("first-user", "second-user"));
        when(chatSessionRegistry.getUserIds("room")).thenReturn(userIds);

        chatSessionEventHandler.handle(ChatSessionEvent.delete("room"));

        ChatRoomEventResponse response = ChatRoomEventResponse.deleted("room");
        InOrder inOrder = inOrder(webSocketUserMessageSender, chatSessionRegistry);
        inOrder.verify(webSocketUserMessageSender).send("first-user", "/channel/events", response);
        inOrder.verify(webSocketUserMessageSender).send("second-user", "/channel/events", response);
        inOrder.verify(chatSessionRegistry).delete("room");
    }

    // Checks that one failed notification does not block other participants or cached room cleanup.
    @Test
    void continueDeletingRoomAfterNotificationFailure() {
        ChatRoomEventResponse response = ChatRoomEventResponse.deleted("room");
        LinkedHashSet<String> userIds = new LinkedHashSet<>(List.of("first-user", "second-user"));
        when(chatSessionRegistry.getUserIds("room")).thenReturn(userIds);
        doThrow(new IllegalStateException("delivery failed"))
            .when(webSocketUserMessageSender)
            .send("first-user", "/channel/events", response);

        chatSessionEventHandler.handle(ChatSessionEvent.delete("room"));

        verify(webSocketUserMessageSender).send("second-user", "/channel/events", response);
        verify(chatSessionRegistry).delete("room");
    }

    // Creates a text channel returned by the access service.
    private TextChannel createTextChannel() {
        return mock(TextChannel.class);
    }
}
