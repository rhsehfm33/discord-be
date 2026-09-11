package discord.chat.api.application.message;

import discord.chat.api.infrastructure.websocket.ChatSessionRegistry;
import discord.chat.api.infrastructure.websocket.WebSocketSessionMessageSender;
import discord.chat.api.interfaces.message.ChatMessageResponse;
import discord.chat.api.interfaces.message.MessageSenderResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ChatRoomMessageDeliveryTest {
    // Checks that the message is sent to every session returned for its room.
    @Test
    void deliverMessageToRoomSessions() {
        ChatSessionRegistry chatSessionRegistry = mock(ChatSessionRegistry.class);
        WebSocketSessionMessageSender webSocketSessionMessageSender = mock(WebSocketSessionMessageSender.class);
        ChatRoomMessageDelivery chatRoomMessageDelivery =
            new ChatRoomMessageDelivery(chatSessionRegistry, webSocketSessionMessageSender);
        when(chatSessionRegistry.getSessionIds("room")).thenReturn(Set.of("first-session", "second-session"));

        ChatMessageResponse chatMessageResponse = new ChatMessageResponse(
            "message",
            "room",
            "channel",
            new MessageSenderResponse("sender", "Sender", null),
            "hello",
            Instant.parse("2026-09-07T00:00:00Z")
        );

        chatRoomMessageDelivery.deliver(chatMessageResponse);

        verify(chatSessionRegistry).getSessionIds("room");
        verify(webSocketSessionMessageSender).send("first-session", "/channel", chatMessageResponse);
        verify(webSocketSessionMessageSender).send("second-session", "/channel", chatMessageResponse);
        verifyNoMoreInteractions(chatSessionRegistry, webSocketSessionMessageSender);
    }
}
