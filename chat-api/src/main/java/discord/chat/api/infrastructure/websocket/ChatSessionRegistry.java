package discord.chat.api.infrastructure.websocket;

import discord.chat.api.domain.chat.channel.ChannelAccessService;
import discord.chat.api.infrastructure.redis.ChatMessageRedisBroker;
import discord.chat.api.infrastructure.redis.DirectMessageRedisBroker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ChatSessionRegistry {
    private final ChatMessageRedisBroker chatMessageRedisBroker;
    private final DirectMessageRedisBroker directMessageRedisBroker;
    private final ChannelAccessService channelAccessService;
    private final Map<String, Set<String>> chatRoomIdToUserIds = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userIdToChatRoomIds = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userIdToSessionIds = new HashMap<>();

    public synchronized void register(String userId, String sessionId) {
        List<String> chatRoomIds = channelAccessService.getChatRoomIdsBy(userId);
        Set<String> sessionIds = userIdToSessionIds.computeIfAbsent(
            userId, key -> new HashSet<>()
        );

        boolean firstSession = sessionIds.isEmpty();
        if (!sessionIds.add(sessionId) || !firstSession) {
            return;
        }

        userIdToChatRoomIds.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet());
        try {
            for (String chatRoomId : chatRoomIds) {
                addAccess(userId, chatRoomId);
            }
            directMessageRedisBroker.subscribe(userId);
        } catch (RuntimeException exception) {
            removeSession(userId, sessionId);
            throw exception;
        }
    }

    public synchronized void join(String userId, String chatRoomId) {
        if (!hasUserSessions(userId)) {
            return;
        }

        if (channelAccessService.hasChatRoomAccess(userId, chatRoomId)) {
            addAccess(userId, chatRoomId);
        }
    }

    public synchronized void leave(String userId, String chatRoomId) {
        Set<String> chatRoomIds = userIdToChatRoomIds.get(userId);
        if (chatRoomIds == null || !chatRoomIds.remove(chatRoomId)) {
            return;
        }

        removeUserFromChatRoom(userId, chatRoomId);
    }

    public synchronized void delete(String chatRoomId) {
        Set<String> userIds = chatRoomIdToUserIds.remove(chatRoomId);
        if (userIds == null) {
            return;
        }

        for (String userId : userIds) {
            userIdToChatRoomIds.get(userId).remove(chatRoomId);
        }
        chatMessageRedisBroker.unsubscribe(chatRoomId);
    }

    private boolean hasUserSessions(String userId) {
        Set<String> sessionIds = userIdToSessionIds.get(userId);
        return sessionIds != null && !sessionIds.isEmpty();
    }

    public boolean hasRoomAccess(String userId, String chatRoomId) {
        Set<String> chatRoomIds = userIdToChatRoomIds.get(userId);
        return chatRoomIds != null && chatRoomIds.contains(chatRoomId);
    }

    public synchronized void removeSession(String userId, String sessionId) {
        Set<String> sessionIds = userIdToSessionIds.get(userId);
        if (sessionIds == null || !sessionIds.remove(sessionId)) {
            return;
        }
        if (!sessionIds.isEmpty()) {
            return;
        }
        userIdToSessionIds.remove(userId);

        Set<String> chatRoomIds = userIdToChatRoomIds.remove(userId);
        if (chatRoomIds == null) {
            return;
        }
        for (String chatRoomId : chatRoomIds) {
            removeUserFromChatRoom(userId, chatRoomId);
        }
        directMessageRedisBroker.unsubscribe(userId);
    }

    public Set<String> getUserIds(String chatRoomId) {
        Set<String> userIds = chatRoomIdToUserIds.get(chatRoomId);
        if (userIds == null) {
            return Set.of();
        }

        return Set.copyOf(userIds);
    }

    private void addAccess(String userId, String chatRoomId) {
        if (!chatRoomIdToUserIds.containsKey(chatRoomId)) {
            chatMessageRedisBroker.subscribe(chatRoomId);
            chatRoomIdToUserIds.put(chatRoomId, ConcurrentHashMap.newKeySet());
        } else {
            chatMessageRedisBroker.requireConnection();
        }

        userIdToChatRoomIds.get(userId).add(chatRoomId);
        chatRoomIdToUserIds.get(chatRoomId).add(userId);
    }

    private void removeUserFromChatRoom(String userId, String chatRoomId) {
        Set<String> userIds = chatRoomIdToUserIds.get(chatRoomId);
        userIds.remove(userId);
        if (userIds.isEmpty()) {
            chatRoomIdToUserIds.remove(chatRoomId);
            chatMessageRedisBroker.unsubscribe(chatRoomId);
        }
    }
}
