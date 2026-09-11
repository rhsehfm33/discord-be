package discord.chat.message.infrastructure.websocket;

import discord.chat.message.domain.chat.channel.ChannelAccessService;
import discord.chat.message.interfaces.chat.channel.AccessibleTextChannelResponse;
import discord.chat.common.infrastructure.user.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketAuthorizationInterceptorTest {
    // Checks that a socket connection resolves channel access locally and registers authorized channels.
    @Test
    void registersChannelsFromLocalAccessService() {
        ChannelAccessService channelAccessService = mock(ChannelAccessService.class);
        ChatSessionRegistry chatSessionRegistry = mock(ChatSessionRegistry.class);
        var interceptor = new WebSocketAuthorizationInterceptor(channelAccessService, chatSessionRegistry);
        var accessibleChannel = new AccessibleTextChannelResponse("room", "channel");
        when(channelAccessService.getAccessibleTextChannels("user")).thenReturn(List.of(accessibleChannel));
        User user = new User("user", "User", "user@example.com", null, null);
        var stompHeaders = StompHeaderAccessor.create(StompCommand.CONNECT);
        stompHeaders.setSessionId("session");
        stompHeaders.setUser(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        var message = MessageBuilder.createMessage(new byte[0], stompHeaders.getMessageHeaders());

        interceptor.preSend(message, new ExecutorSubscribableChannel());

        verify(channelAccessService).getAccessibleTextChannels("user");
        verify(chatSessionRegistry).register("session", Set.of(accessibleChannel));
    }

    // Checks that clients cannot send to non-existing destinations.
    @Test
    void rejectInvalidSendDestination() {
        ChannelAccessService channelAccessService = mock(ChannelAccessService.class);
        ChatSessionRegistry chatSessionRegistry = mock(ChatSessionRegistry.class);
        var webSocketAuthorizationInterceptor = new WebSocketAuthorizationInterceptor(
            channelAccessService, chatSessionRegistry
        );

        for (String blockedDestination : List.of("/channel", "/user/sender/channel/errors")) {
            var stompHeaders = StompHeaderAccessor.create(StompCommand.SEND);
            stompHeaders.setDestination(blockedDestination);
            var stompMessage = MessageBuilder.createMessage(new byte[0], stompHeaders.getMessageHeaders());
            var inboundMessageChannel = new ExecutorSubscribableChannel();

            assertThatThrownBy(() -> webSocketAuthorizationInterceptor.preSend(stompMessage, inboundMessageChannel))
                .isInstanceOf(IllegalArgumentException.class);
        }
        verifyNoInteractions(channelAccessService, chatSessionRegistry);
    }
}
