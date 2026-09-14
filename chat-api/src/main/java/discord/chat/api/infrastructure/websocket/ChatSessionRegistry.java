package discord.chat.api.infrastructure.websocket;

import discord.chat.api.infrastructure.redis.ChatMessageRedisBroker;
import discord.chat.common.infrastructure.chat.channel.TextChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ChatSessionRegistry {
    private final ChatMessageRedisBroker redisBroker;
    private final Map<String, Set<String>> chatRoomToChannels = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> listenerToChatRooms = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> chatRoomToListeners = new ConcurrentHashMap<>();

    public synchronized void register(String sessionId, List<TextChannel> accessibleChannels) {
        if (listenerToChatRooms.containsKey(sessionId)) {
            return;
        }
        listenerToChatRooms.put(sessionId, ConcurrentHashMap.newKeySet());

        for (TextChannel textChannel : accessibleChannels) {
            String textChannelId = textChannel.getId();
            String chatRoomId = textChannel.getChatRoom().getId();

            try {
                if (!chatRoomToListeners.containsKey(chatRoomId)) {
                    redisBroker.subscribe(chatRoomId);
                    chatRoomToListeners.put(chatRoomId, ConcurrentHashMap.newKeySet());
                    chatRoomToChannels.put(chatRoomId, ConcurrentHashMap.newKeySet());
                } else {
                    redisBroker.requireConnection();
                }

                chatRoomToChannels.get(chatRoomId).add(textChannelId);
                listenerToChatRooms.get(sessionId).add(chatRoomId);
                chatRoomToListeners.get(chatRoomId).add(sessionId);
            } catch (RuntimeException exception) {
                removeSession(sessionId);
                throw exception;
            }
        }
    }

    public synchronized boolean hasChannelAccess(String sessionId, String chatRoomId, String textChannelId) {
        return listenerToChatRooms.containsKey(sessionId)
                && listenerToChatRooms.get(sessionId).contains(chatRoomId)
                && chatRoomToChannels.containsKey(chatRoomId)
                && chatRoomToChannels.get(chatRoomId).contains(textChannelId);
    }

    public synchronized void removeSession(String sessionId) {
        Set<String> chatRoomIds = listenerToChatRooms.remove(sessionId);
        if (chatRoomIds == null) {
            return;
        }
        for (String chatRoomId : chatRoomIds) {
            chatRoomToListeners.get(chatRoomId).remove(sessionId);
            if (chatRoomToListeners.get(chatRoomId).isEmpty()) {
                chatRoomToListeners.remove(chatRoomId);
                chatRoomToChannels.remove(chatRoomId);
                redisBroker.unsubscribe(chatRoomId);
            }
        }
    }

    public synchronized Set<String> getSessionIds(String chatRoomId) {
        Set<String> sessionIds = chatRoomToListeners.get(chatRoomId);
        if (sessionIds == null) {
            return Set.of();
        }

        return Set.copyOf(sessionIds);
    }
}
