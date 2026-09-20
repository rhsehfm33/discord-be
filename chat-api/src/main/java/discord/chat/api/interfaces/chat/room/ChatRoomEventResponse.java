package discord.chat.api.interfaces.chat.room;

public record ChatRoomEventResponse(
    Type type,
    String chatRoomId
) {
    public enum Type {
        SUBSCRIBED,
        UNSUBSCRIBED,
        DELETED
    }

    public static ChatRoomEventResponse subscribed(String chatRoomId) {
        return new ChatRoomEventResponse(Type.SUBSCRIBED, chatRoomId);
    }

    public static ChatRoomEventResponse unsubscribed(String chatRoomId) {
        return new ChatRoomEventResponse(Type.UNSUBSCRIBED, chatRoomId);
    }

    public static ChatRoomEventResponse deleted(String chatRoomId) {
        return new ChatRoomEventResponse(Type.DELETED, chatRoomId);
    }
}
