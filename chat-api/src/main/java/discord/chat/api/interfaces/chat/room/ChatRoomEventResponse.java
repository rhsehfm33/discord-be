package discord.chat.api.interfaces.chat.room;

public record ChatRoomEventResponse(
    Type type,
    String chatRoomId
) {
    public enum Type {
        DELETED
    }

    public static ChatRoomEventResponse deleted(String chatRoomId) {
        return new ChatRoomEventResponse(Type.DELETED, chatRoomId);
    }
}
