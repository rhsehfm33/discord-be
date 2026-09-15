package discord.chat.api.application.session;

public record ChatSessionEvent(
    Type type,
    String userId,
    String chatRoomId
) {
    public enum Type {
        JOIN,
        LEAVE,
        DELETE
    }

    public static ChatSessionEvent join(String userId, String chatRoomId) {
        return new ChatSessionEvent(Type.JOIN, userId, chatRoomId);
    }

    public static ChatSessionEvent leave(String userId, String chatRoomId) {
        return new ChatSessionEvent(Type.LEAVE, userId, chatRoomId);
    }

    public static ChatSessionEvent delete(String chatRoomId) {
        return new ChatSessionEvent(Type.DELETE, null, chatRoomId);
    }
}
