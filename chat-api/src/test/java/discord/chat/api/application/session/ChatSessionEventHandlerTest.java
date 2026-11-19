package discord.chat.api.application.session;

import discord.chat.api.application.session.ChatSessionEventHandler.PersonalJoinReceived;
import discord.chat.api.infrastructure.websocket.ChatSessionRegistry;
import discord.chat.api.infrastructure.websocket.WebSocketUserMessageSender;
import discord.chat.api.interfaces.chat.room.ChatRoomEventResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatSessionEventHandlerTest {
    @Mock
    private ChatSessionRegistry chatSessionRegistry;
    @Mock
    private WebSocketUserMessageSender webSocketUserMessageSender;

    private ChatSessionEventHandler chatSessionEventHandler;

    // Creates the event handler with mocked dependencies.
    @BeforeEach
    void setUp() {
        chatSessionEventHandler = new ChatSessionEventHandler(
            chatSessionRegistry,
            webSocketUserMessageSender
        );
    }

    // Checks that a personal join identifies the user and room to the registry.
    @Test
    void joinAccessibleRoom() {
        chatSessionEventHandler.join(new PersonalJoinReceived("user", "joined-room"));

        verify(chatSessionRegistry).join("user", "joined-room");
    }

    // Checks that a leave event removes the user's cached room access.
    @Test
    void leaveRoom() {
        chatSessionEventHandler.handleRoomEvent(ChatSessionEvent.leave("user", "nickname", "room"));

        verify(chatSessionRegistry).leave("user", "room");
    }

    // Checks that a delete event notifies local participants before removing cached room access.
    @Test
    void deleteRoom() {
        LinkedHashSet<String> userIds = new LinkedHashSet<>(List.of("first-user", "second-user"));
        when(chatSessionRegistry.getUserIds("room")).thenReturn(userIds);

        chatSessionEventHandler.handleRoomEvent(ChatSessionEvent.delete("room"));

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

        chatSessionEventHandler.handleRoomEvent(ChatSessionEvent.delete("room"));

        verify(webSocketUserMessageSender).send("second-user", "/channel/events", response);
        verify(chatSessionRegistry).delete("room");
    }
}
