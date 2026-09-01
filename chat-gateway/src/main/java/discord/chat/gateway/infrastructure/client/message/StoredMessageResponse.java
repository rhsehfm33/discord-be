package discord.chat.gateway.infrastructure.client.message;

import java.time.Instant;

public record StoredMessageResponse(
    String messageId,
    String senderId,
    String chatRoomId,
    String textChannelId,
    String content,
    Instant createdAt
) {
}
