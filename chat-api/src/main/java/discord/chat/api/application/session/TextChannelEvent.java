package discord.chat.api.application.session;

public record TextChannelEvent(
    Type type,
    String chatRoomId,
    String textChannelId,
    String title,
    String ownerId
) {
    public enum Type {
        TEXT_CHANNEL_CREATED,
        TEXT_CHANNEL_UPDATED,
        TEXT_CHANNEL_DELETED
    }
}
