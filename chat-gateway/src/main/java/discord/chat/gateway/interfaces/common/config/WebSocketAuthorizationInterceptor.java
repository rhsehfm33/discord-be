package discord.chat.gateway.interfaces.common.config;

import java.util.Set;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import discord.chat.common.infrastructure.user.User;
import discord.chat.gateway.infrastructure.client.chatapi.AccessibleTextChannelResponse;
import discord.chat.gateway.infrastructure.client.chatapi.ChatApiClient;
import discord.chat.gateway.infrastructure.websocket.WebSocketSessionAuthorizationRegistry;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebSocketAuthorizationInterceptor implements ChannelInterceptor {
    private static final String PERSONAL_CHANNEL_DESTINATION = "/user/channel";

    private final ChatApiClient chatApiClient;
    private final WebSocketSessionAuthorizationRegistry sessionAuthorizationRegistry;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            registerSession(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            validateSubscription(accessor);
        } else if (StompCommand.DISCONNECT.equals(command)) {
            sessionAuthorizationRegistry.remove(accessor.getSessionId());
        }

        return message;
    }

    private void registerSession(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof Authentication authentication)
            || !(authentication.getPrincipal() instanceof User user)) {
            throw new IllegalStateException("Authenticated WebSocket user is required");
        }

        Set<AccessibleTextChannelResponse> channels = Set.copyOf(
            chatApiClient.getAccessibleTextChannels(user.getId())
        );
        sessionAuthorizationRegistry.register(accessor.getSessionId(), channels);
    }

    private void validateSubscription(StompHeaderAccessor accessor) {
        if (!PERSONAL_CHANNEL_DESTINATION.equals(accessor.getDestination())) {
            throw new IllegalArgumentException("Only the personal channel destination may be subscribed");
        }
    }
}
