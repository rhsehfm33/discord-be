package discord.chat.api.infrastructure.websocket;

import discord.chat.common.infrastructure.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class WebSocketAuthorizationInterceptorTest {
    // Checks that a socket connection registers the authenticated user and session.
    @Test
    void registersAuthenticatedSession() {
        ChatSessionRegistry chatSessionRegistry = mock(ChatSessionRegistry.class);
        var interceptor = new WebSocketAuthorizationInterceptor(chatSessionRegistry);
        User user = new User("user", "User", "user@example.com", null, null);
        var stompHeaders = StompHeaderAccessor.create(StompCommand.CONNECT);
        stompHeaders.setSessionId("session");
        stompHeaders.setSessionAttributes(new HashMap<>());
        stompHeaders.setUser(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        var message = MessageBuilder.createMessage(new byte[0], stompHeaders.getMessageHeaders());

        interceptor.preSend(message, new ExecutorSubscribableChannel());

        verify(chatSessionRegistry).register("user", "session");
        assertThat(stompHeaders.getSessionAttributes()).containsEntry("userId", "user");
    }

    // Checks that disconnect cleanup resolves the user from WebSocket session attributes.
    @Test
    void removeDisconnectedSessionForStoredUser() {
        ChatSessionRegistry chatSessionRegistry = mock(ChatSessionRegistry.class);
        var webSocketAuthorizationInterceptor = new WebSocketAuthorizationInterceptor(chatSessionRegistry);
        var stompHeaders = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        stompHeaders.setSessionId("session");
        stompHeaders.setSessionAttributes(new HashMap<>(Map.of("userId", "user")));
        var message = MessageBuilder.createMessage(new byte[0], stompHeaders.getMessageHeaders());

        webSocketAuthorizationInterceptor.preSend(message, new ExecutorSubscribableChannel());

        verify(chatSessionRegistry).removeSession("user", "session");
    }

    // Checks that clients cannot send to non-existing destinations.
    @Test
    void rejectInvalidSendDestination() {
        ChatSessionRegistry chatSessionRegistry = mock(ChatSessionRegistry.class);
        var webSocketAuthorizationInterceptor = new WebSocketAuthorizationInterceptor(chatSessionRegistry);

        for (String blockedDestination : List.of("/channel", "/user/sender/channel/errors")) {
            var stompHeaders = StompHeaderAccessor.create(StompCommand.SEND);
            stompHeaders.setDestination(blockedDestination);
            var stompMessage = MessageBuilder.createMessage(new byte[0], stompHeaders.getMessageHeaders());
            var inboundMessageChannel = new ExecutorSubscribableChannel();

            assertThatThrownBy(() -> webSocketAuthorizationInterceptor.preSend(stompMessage, inboundMessageChannel))
                .isInstanceOf(IllegalArgumentException.class);
        }
        verifyNoInteractions(chatSessionRegistry);
    }
}
