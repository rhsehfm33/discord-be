package discord.chat.message.interfaces.message;

public record MessagePublishErrorResponse(
    String code,
    String messageId,
    String chatRoomId,
    String textChannelId,
    String message
) {}
