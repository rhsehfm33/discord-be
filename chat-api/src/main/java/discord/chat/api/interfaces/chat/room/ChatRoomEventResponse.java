package discord.chat.api.interfaces.chat.room;

public record ChatRoomEventResponse(
    Type type,
    String chatRoomId,
    ChatRoomParticipantResponse participant
) {
    public enum Type {
        SUBSCRIBED,
        UNSUBSCRIBED,
        DELETED
    }

    public static ChatRoomEventResponse subscribed(String chatRoomId, ChatRoomParticipantResponse participant) {
        return new ChatRoomEventResponse(Type.SUBSCRIBED, chatRoomId, participant);
    }

    public static ChatRoomEventResponse unsubscribed(String chatRoomId, ChatRoomParticipantResponse participant) {
        return new ChatRoomEventResponse(Type.UNSUBSCRIBED, chatRoomId, participant);
    }

    public static ChatRoomEventResponse deleted(String chatRoomId) {
        return new ChatRoomEventResponse(Type.DELETED, chatRoomId, null);
    }
}
