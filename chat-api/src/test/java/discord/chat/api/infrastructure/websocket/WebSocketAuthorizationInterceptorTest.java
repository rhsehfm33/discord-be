package discord.chat.api.infrastructure.websocket;

import discord.chat.api.domain.chat.channel.ChannelAccessService;
import discord.chat.common.infrastructure.chat.channel.TextChannel;
import discord.chat.common.infrastructure.chat.room.ChatRoom;
import discord.chat.common.infrastructure.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class WebSocketAuthorizationInterceptorTest {
    // Checks that a socket connection resolves channel access locally and registers authorized channels.
    @Test
    void registersChannelsFromLocalAccessService() {
        ChannelAccessService channelAccessService = mock(ChannelAccessService.class);
        ChatSessionRegistry chatSessionRegistry = mock(ChatSessionRegistry.class);
        var interceptor = new WebSocketAuthorizationInterceptor(channelAccessService, chatSessionRegistry);
        TextChannel accessibleChannel = mock(TextChannel.class);
        ChatRoom chatRoom = mock(ChatRoom.class);
        when(accessibleChannel.getId()).thenReturn("channel");
        when(accessibleChannel.getChatRoom()).thenReturn(chatRoom);
        when(chatRoom.getId()).thenReturn("room");
        List<TextChannel> accessibleChannels = List.of(accessibleChannel);
        when(channelAccessService.getTextChannelsBy("user")).thenReturn(accessibleChannels);
        User user = new User("user", "User", "user@example.com", null, null);
        var stompHeaders = StompHeaderAccessor.create(StompCommand.CONNECT);
        stompHeaders.setSessionId("session");
        stompHeaders.setUser(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        var message = MessageBuilder.createMessage(new byte[0], stompHeaders.getMessageHeaders());

        interceptor.preSend(message, new ExecutorSubscribableChannel());

        verify(channelAccessService).getTextChannelsBy("user");
        verify(chatSessionRegistry).register("user", "session", accessibleChannels);
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
