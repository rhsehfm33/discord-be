package discord.chat.message.infrastructure.websocket;

import discord.chat.message.interfaces.chat.channel.AccessibleTextChannelResponse;
import discord.chat.message.infrastructure.redis.ChatMessageRedisBroker;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ChatSessionRegistry {
    private final ChatMessageRedisBroker redisBroker;
    private final Map<String, Map<String, String>> sessionToChannels = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> chatRoomIdToSessions = new ConcurrentHashMap<>();

    public synchronized void register(
        String sessionId,
        Set<AccessibleTextChannelResponse> accessibleChannels
    ) {
        if (sessionToChannels.containsKey(sessionId)) {
            return;
        }
        Map<String, String> channels = accessibleChannels.stream().collect(Collectors.toUnmodifiableMap(
            AccessibleTextChannelResponse::textChannelId, AccessibleTextChannelResponse::chatRoomId
        ));
        Set<String> registeredRooms = new HashSet<>();
        try {
            for (String chatRoomId : Set.copyOf(channels.values())) {
                Set<String> roomSessions = chatRoomIdToSessions.computeIfAbsent(
                    chatRoomId, ignored -> ConcurrentHashMap.newKeySet()
                );
                if (roomSessions.isEmpty()) {
                    redisBroker.subscribe(chatRoomId);
                } else {
                    redisBroker.requireConnection();
                }
                roomSessions.add(sessionId);
                registeredRooms.add(chatRoomId);
            }
            sessionToChannels.put(sessionId, channels);
        } catch (RuntimeException exception) {
            removeFromRooms(sessionId, registeredRooms);
            throw exception;
        }
    }

    public Optional<String> getAuthorizedChatRoomId(String sessionId, String textChannelId) {
        Map<String, String> channels = sessionToChannels.get(sessionId);
        if (channels == null) {
            return Optional.empty();
        }

        String chatRoomId = channels.get(textChannelId);
        return Optional.ofNullable(chatRoomId);
    }

    public synchronized void remove(String sessionId) {
        Map<String, String> channels = sessionToChannels.remove(sessionId);
        if (channels != null) {
            removeFromRooms(sessionId, Set.copyOf(channels.values()));
        }
    }

    private void removeFromRooms(String sessionId, Set<String> chatRoomIds) {
        for (String chatRoomId : chatRoomIds) {
            Set<String> roomSessions = chatRoomIdToSessions.get(chatRoomId);
            roomSessions.remove(sessionId);
            if (roomSessions.isEmpty()) {
                chatRoomIdToSessions.remove(chatRoomId);
                redisBroker.unsubscribe(chatRoomId);
            }
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        remove(event.getSessionId());
    }

    public synchronized Set<String> getSessionIds(String chatRoomId) {
        Set<String> sessionIds = chatRoomIdToSessions.get(chatRoomId);
        if (sessionIds == null) {
            return Set.of();
        }

        return Set.copyOf(sessionIds);
    }
}
