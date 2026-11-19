package discord.chat.api.interfaces.chat.room;

import discord.chat.api.interfaces.chat.channel.TextChannelResponse;

public record ChatRoomEventResponse(
    Type type,
    String chatRoomId,
    ChatRoomParticipantResponse participant,
    TextChannelResponse textChannel
) {
    public enum Type {
        SUBSCRIBED,
        UNSUBSCRIBED,
        DELETED,
        TEXT_CHANNEL_CREATED,
        TEXT_CHANNEL_UPDATED,
        TEXT_CHANNEL_DELETED
    }

    public static ChatRoomEventResponse subscribed(String chatRoomId, ChatRoomParticipantResponse participant) {
        return new ChatRoomEventResponse(Type.SUBSCRIBED, chatRoomId, participant, null);
    }

    public static ChatRoomEventResponse unsubscribed(String chatRoomId, ChatRoomParticipantResponse participant) {
        return new ChatRoomEventResponse(Type.UNSUBSCRIBED, chatRoomId, participant, null);
    }

    public static ChatRoomEventResponse deleted(String chatRoomId) {
        return new ChatRoomEventResponse(Type.DELETED, chatRoomId, null, null);
    }

    public static ChatRoomEventResponse textChannelChanged(Type type, TextChannelResponse textChannel) {
        return new ChatRoomEventResponse(type, textChannel.getChatRoomId(), null, textChannel);
    }
}
