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
    private final Map<String, Set<String>> chatRoomIdToUserIds = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userIdToChatRoomIds = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userIdToSessionIds = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> chatRoomIdToTextChannelIds = new ConcurrentHashMap<>();

    public synchronized void register(String userId, String sessionId, List<TextChannel> accessibleChannels) {
        Set<String> sessionIds = userIdToSessionIds.computeIfAbsent(
            userId, key -> ConcurrentHashMap.newKeySet()
        );
        boolean firstSession = sessionIds.isEmpty();
        if (!sessionIds.add(sessionId)) {
            return;
        }
        if (!firstSession) {
            return;
        }
        userIdToChatRoomIds.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet());

        for (TextChannel textChannel : accessibleChannels) {
            try {
                addAccess(userId, textChannel);
            } catch (RuntimeException exception) {
                removeSession(userId, sessionId);
                throw exception;
            }
        }
    }

    public synchronized void join(String userId, List<TextChannel> accessibleChannels) {
        if (!hasUserSessions(userId)) {
            return;
        }

        for (TextChannel textChannel : accessibleChannels) {
            addAccess(userId, textChannel);
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
        chatRoomIdToTextChannelIds.remove(chatRoomId);
        chatMessageRedisBroker.unsubscribe(chatRoomId);
    }

    public synchronized boolean hasUserSessions(String userId) {
        Set<String> sessionIds = userIdToSessionIds.get(userId);
        return sessionIds != null && !sessionIds.isEmpty();
    }

    public synchronized boolean hasChannelAccess(String userId, String chatRoomId, String textChannelId) {
        return userIdToChatRoomIds.containsKey(userId)
            && userIdToChatRoomIds.get(userId).contains(chatRoomId)
            && chatRoomIdToTextChannelIds.containsKey(chatRoomId)
            && chatRoomIdToTextChannelIds.get(chatRoomId).contains(textChannelId);
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
    }

    public synchronized Set<String> getUserIds(String chatRoomId) {
        Set<String> userIds = chatRoomIdToUserIds.get(chatRoomId);
        if (userIds == null) {
            return Set.of();
        }

        return Set.copyOf(userIds);
    }

    private void addAccess(String userId, TextChannel textChannel) {
        String textChannelId = textChannel.getId();
        String chatRoomId = textChannel.getChatRoom().getId();

        if (!chatRoomIdToUserIds.containsKey(chatRoomId)) {
            chatMessageRedisBroker.subscribe(chatRoomId);
            chatRoomIdToUserIds.put(chatRoomId, ConcurrentHashMap.newKeySet());
            chatRoomIdToTextChannelIds.put(chatRoomId, ConcurrentHashMap.newKeySet());
        } else {
            chatMessageRedisBroker.requireConnection();
        }

        chatRoomIdToTextChannelIds.get(chatRoomId).add(textChannelId);
        userIdToChatRoomIds.get(userId).add(chatRoomId);
        chatRoomIdToUserIds.get(chatRoomId).add(userId);
    }

    private void removeUserFromChatRoom(String userId, String chatRoomId) {
        Set<String> userIds = chatRoomIdToUserIds.get(chatRoomId);
        userIds.remove(userId);
        if (userIds.isEmpty()) {
            chatRoomIdToUserIds.remove(chatRoomId);
            chatRoomIdToTextChannelIds.remove(chatRoomId);
            chatMessageRedisBroker.unsubscribe(chatRoomId);
        }
    }
}
