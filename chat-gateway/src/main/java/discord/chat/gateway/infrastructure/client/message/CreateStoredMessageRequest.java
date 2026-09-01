package discord.chat.gateway.infrastructure.client.message;

public record CreateStoredMessageRequest(
    String senderId,
    String chatRoomId,
    String textChannelId,
    String content
) {
}
