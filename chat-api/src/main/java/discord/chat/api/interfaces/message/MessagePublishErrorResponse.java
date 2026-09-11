package discord.chat.api.interfaces.message;

public record MessagePublishErrorResponse(
    String code,
    String messageId,
    String chatRoomId,
    String textChannelId,
    String message
) {}
