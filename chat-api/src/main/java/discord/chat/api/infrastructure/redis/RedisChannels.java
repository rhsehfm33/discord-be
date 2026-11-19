package discord.chat.api.infrastructure.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class RedisChannels {
    private final String roomPrefix;
    private final String personalPrefix;

    RedisChannels(
        @Value("${messaging.redis.chat-room-message-topic}") String roomTopic,
        @Value("${messaging.redis.direct-message-topic}") String personalTopic
    ) {
        this.roomPrefix = roomTopic + ":";
        this.personalPrefix = personalTopic + ":";
    }

    String room(String chatRoomId) {
        return roomPrefix + chatRoomId;
    }

    String personal(String userId) {
        return personalPrefix + userId;
    }

    boolean isRoom(String channel) {
        return channel.startsWith(roomPrefix);
    }

    boolean isPersonal(String channel) {
        return channel.startsWith(personalPrefix);
    }

    String personalUserId(String channel) {
        return channel.substring(personalPrefix.length());
    }
}
