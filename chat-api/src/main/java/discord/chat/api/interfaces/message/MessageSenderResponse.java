package discord.chat.api.interfaces.message;

public record MessageSenderResponse(
    String id,
    String nickName,
    String imageUrl
) {
}
