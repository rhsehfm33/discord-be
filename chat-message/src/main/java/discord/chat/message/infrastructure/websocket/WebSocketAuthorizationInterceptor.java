package discord.chat.message.infrastructure.websocket;

import java.util.Set;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import discord.chat.common.infrastructure.user.User;
import discord.chat.message.interfaces.chat.channel.AccessibleTextChannelResponse;
import discord.chat.message.domain.chat.channel.ChannelAccessService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebSocketAuthorizationInterceptor implements ChannelInterceptor {
    private static final String PERSONAL_CHANNEL_DESTINATION = "/user/channel";

    private final ChannelAccessService channelAccessService;
    private final ChatSessionRegistry chatSessionRegistry;

    @Override
    @NonNull
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            registerSession(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            validateSubscription(accessor);
        } else if (StompCommand.SEND.equals(command)
            && !"/app/sendText".equals(accessor.getDestination())) {
            throw new IllegalArgumentException("Messages must be sent through /app/sendText");
        } else if (StompCommand.DISCONNECT.equals(command)) {
            chatSessionRegistry.remove(accessor.getSessionId());
        }

        return message;
    }

    @Override
    public void afterSendCompletion(
        @NonNull Message<?> message,
        @NonNull MessageChannel channel,
        boolean sent,
        @Nullable Exception exception
    ) {
        if (sent && exception == null) {
            return;
        }

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (!StompCommand.CONNECT.equals(accessor.getCommand())) {
            return;
        }

        chatSessionRegistry.remove(accessor.getSessionId());
    }

    private void registerSession(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof Authentication authentication)
            || !(authentication.getPrincipal() instanceof User user)) {
            throw new IllegalStateException("Authenticated WebSocket user is required");
        }

        Set<AccessibleTextChannelResponse> channels = Set.copyOf(
            channelAccessService.getAccessibleTextChannels(user.getId())
        );
        chatSessionRegistry.register(accessor.getSessionId(), channels);
    }

    private void validateSubscription(StompHeaderAccessor accessor) {
        if (!PERSONAL_CHANNEL_DESTINATION.equals(accessor.getDestination())
            && !"/user/channel/errors".equals(accessor.getDestination())) {
            throw new IllegalArgumentException("Only the personal channel destination may be subscribed");
        }
    }
}
