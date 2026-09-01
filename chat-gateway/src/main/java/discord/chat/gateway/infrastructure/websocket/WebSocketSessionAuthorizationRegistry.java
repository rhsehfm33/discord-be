package discord.chat.gateway.infrastructure.websocket;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import discord.chat.gateway.infrastructure.client.chatapi.AccessibleTextChannelResponse;

@Component
public class WebSocketSessionAuthorizationRegistry {
    private final Map<String, SubscribedChannels> subscribedChannelsBySessionId =
        new ConcurrentHashMap<>();

    public void register(
        String sessionId,
        Set<AccessibleTextChannelResponse> accessibleChannels
    ) {
        Map<String, String> chatRoomIdByTextChannelId = accessibleChannels.stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(
                AccessibleTextChannelResponse::textChannelId,
                AccessibleTextChannelResponse::chatRoomId
            ));
        subscribedChannelsBySessionId.put(
            sessionId,
            new SubscribedChannels(chatRoomIdByTextChannelId)
        );
    }

    public Optional<String> getAuthorizedChatRoomId(String sessionId, String textChannelId) {
        SubscribedChannels subscribedChannels = subscribedChannelsBySessionId.get(sessionId);
        if (subscribedChannels == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(
            subscribedChannels.chatRoomIdByTextChannelId().get(textChannelId)
        );
    }

    public void remove(String sessionId) {
        subscribedChannelsBySessionId.remove(sessionId);
    }

    private record SubscribedChannels(
        Map<String, String> chatRoomIdByTextChannelId
    ) {
    }
}
