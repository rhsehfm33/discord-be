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
    private final ChatMessageRedisBroker chatMessageRedisBroker;
    private final Map<String, Set<String>> chatRoomToChannels = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> listenerToChatRooms = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> chatRoomToListeners = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userIdToSessionIds = new ConcurrentHashMap<>();
    private final Map<String, String> sessionIdToUserId = new ConcurrentHashMap<>();

    public synchronized void register(String userId, String sessionId, List<TextChannel> accessibleChannels) {
        if (listenerToChatRooms.containsKey(sessionId)) {
            return;
        }
        listenerToChatRooms.put(sessionId, ConcurrentHashMap.newKeySet());
        sessionIdToUserId.put(sessionId, userId);
        userIdToSessionIds.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(sessionId);

        for (TextChannel textChannel : accessibleChannels) {
            try {
                addAccess(sessionId, textChannel);
            } catch (RuntimeException exception) {
                removeSession(sessionId);
                throw exception;
            }
        }
    }

    public synchronized void join(String userId, List<TextChannel> accessibleChannels) {
        Set<String> sessionIds = userIdToSessionIds.get(userId);
        if (sessionIds == null) {
            return;
        }

        for (String sessionId : Set.copyOf(sessionIds)) {
            for (TextChannel textChannel : accessibleChannels) {
                addAccess(sessionId, textChannel);
            }
        }
    }

    public synchronized void leave(String userId, String chatRoomId) {
        Set<String> sessionIds = userIdToSessionIds.get(userId);
        if (sessionIds == null) {
            return;
        }

        for (String sessionId : Set.copyOf(sessionIds)) {
            removeAccess(sessionId, chatRoomId);
        }
    }

    public synchronized void delete(String chatRoomId) {
        Set<String> sessionIds = chatRoomToListeners.remove(chatRoomId);
        if (sessionIds == null) {
            return;
        }

        for (String sessionId : sessionIds) {
            listenerToChatRooms.get(sessionId).remove(chatRoomId);
        }
        chatRoomToChannels.remove(chatRoomId);
        chatMessageRedisBroker.unsubscribe(chatRoomId);
    }

    public synchronized boolean hasUserSessions(String userId) {
        Set<String> sessionIds = userIdToSessionIds.get(userId);
        return sessionIds != null && !sessionIds.isEmpty();
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
            removeSessionFromChatRoom(sessionId, chatRoomId);
        }

        String userId = sessionIdToUserId.remove(sessionId);
        Set<String> sessionIds = userIdToSessionIds.get(userId);
        sessionIds.remove(sessionId);
        if (sessionIds.isEmpty()) {
            userIdToSessionIds.remove(userId);
        }
    }

    public synchronized Set<String> getSessionIds(String chatRoomId) {
        Set<String> sessionIds = chatRoomToListeners.get(chatRoomId);
        if (sessionIds == null) {
            return Set.of();
        }

        return Set.copyOf(sessionIds);
    }

    private void addAccess(String sessionId, TextChannel textChannel) {
        String textChannelId = textChannel.getId();
        String chatRoomId = textChannel.getChatRoom().getId();

        if (!chatRoomToListeners.containsKey(chatRoomId)) {
            chatMessageRedisBroker.subscribe(chatRoomId);
            chatRoomToListeners.put(chatRoomId, ConcurrentHashMap.newKeySet());
            chatRoomToChannels.put(chatRoomId, ConcurrentHashMap.newKeySet());
        } else {
            chatMessageRedisBroker.requireConnection();
        }

        chatRoomToChannels.get(chatRoomId).add(textChannelId);
        listenerToChatRooms.get(sessionId).add(chatRoomId);
        chatRoomToListeners.get(chatRoomId).add(sessionId);
    }

    private void removeAccess(String sessionId, String chatRoomId) {
        Set<String> chatRoomIds = listenerToChatRooms.get(sessionId);
        if (chatRoomIds == null || !chatRoomIds.remove(chatRoomId)) {
            return;
        }

        removeSessionFromChatRoom(sessionId, chatRoomId);
    }

    private void removeSessionFromChatRoom(String sessionId, String chatRoomId) {
        Set<String> sessionIds = chatRoomToListeners.get(chatRoomId);
        sessionIds.remove(sessionId);
        if (sessionIds.isEmpty()) {
            chatRoomToListeners.remove(chatRoomId);
            chatRoomToChannels.remove(chatRoomId);
            chatMessageRedisBroker.unsubscribe(chatRoomId);
        }
    }
}
