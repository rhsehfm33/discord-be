package discord.chat.api.application.message;

import discord.chat.api.infrastructure.websocket.ChatSessionRegistry;
import discord.chat.api.infrastructure.websocket.WebSocketUserMessageSender;
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
    // Checks that the message is sent once to every user returned for its room.
    @Test
    void deliverMessageToRoomSessions() {
        ChatSessionRegistry chatSessionRegistry = mock(ChatSessionRegistry.class);
        WebSocketUserMessageSender webSocketUserMessageSender = mock(WebSocketUserMessageSender.class);
        ChatRoomMessageDelivery chatRoomMessageDelivery =
            new ChatRoomMessageDelivery(chatSessionRegistry, webSocketUserMessageSender);
        when(chatSessionRegistry.getUserIds("room")).thenReturn(Set.of("first-user", "second-user"));

        ChatMessageResponse chatMessageResponse = new ChatMessageResponse(
            "message",
            "room",
            "channel",
            new MessageSenderResponse("sender", "Sender", null),
            "hello",
            Instant.parse("2026-09-07T00:00:00Z")
        );

        chatRoomMessageDelivery.deliver(chatMessageResponse);

        verify(chatSessionRegistry).getUserIds("room");
        verify(webSocketUserMessageSender).send("first-user", "/channel", chatMessageResponse);
        verify(webSocketUserMessageSender).send("second-user", "/channel", chatMessageResponse);
        verifyNoMoreInteractions(chatSessionRegistry, webSocketUserMessageSender);
    }
}
