package discord.chat.message.test.unit;

import discord.chat.message.infrastructure.client.chatapi.ChatApiClient;
import discord.chat.message.infrastructure.websocket.ChatSessionRegistry;
import discord.chat.message.infrastructure.websocket.WebSocketAuthorizationInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class WebSocketAuthorizationInterceptorTest {
    // Checks that clients cannot send to non-existing destinations.
    @Test
    void rejectInvalidSendDestination() {
        ChatApiClient chatApiClient = mock(ChatApiClient.class);
        ChatSessionRegistry chatSessionRegistry = mock(ChatSessionRegistry.class);
        var webSocketAuthorizationInterceptor = new WebSocketAuthorizationInterceptor(
            chatApiClient, chatSessionRegistry
        );

        for (String blockedDestination : List.of("/channel", "/user/sender/channel/errors")) {
            var stompHeaders = StompHeaderAccessor.create(StompCommand.SEND);
            stompHeaders.setDestination(blockedDestination);
            var stompMessage = MessageBuilder.createMessage(new byte[0], stompHeaders.getMessageHeaders());
            var inboundMessageChannel = new ExecutorSubscribableChannel();

            assertThatThrownBy(() -> webSocketAuthorizationInterceptor.preSend(stompMessage, inboundMessageChannel))
                .isInstanceOf(IllegalArgumentException.class);
        }
        verifyNoInteractions(chatApiClient, chatSessionRegistry);
    }
}
