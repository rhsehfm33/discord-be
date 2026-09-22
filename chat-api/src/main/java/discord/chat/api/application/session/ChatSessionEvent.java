package discord.chat.api.application.session;

public record ChatSessionEvent(
    Type type,
    String userId,
    String nickName,
    String chatRoomId
) {
    public enum Type {
        JOIN,
        LEAVE,
        DELETE
    }

    public static ChatSessionEvent join(String userId, String nickName, String chatRoomId) {
        return new ChatSessionEvent(Type.JOIN, userId, nickName, chatRoomId);
    }

    public static ChatSessionEvent leave(String userId, String nickName, String chatRoomId) {
        return new ChatSessionEvent(Type.LEAVE, userId, nickName, chatRoomId);
    }

    public static ChatSessionEvent delete(String chatRoomId) {
        return new ChatSessionEvent(Type.DELETE, null, null, chatRoomId);
    }
}
