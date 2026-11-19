package discord.chat.api.interfaces.chat.channel;

import lombok.Getter;
import discord.chat.common.infrastructure.chat.channel.TextChannel;

@Getter
public class TextChannelResponse {
    private final String id;
    private final String title;
    private final String ownerId;
    private final String chatRoomId;

    public TextChannelResponse(TextChannel textChannel) {
        this(
            textChannel.getId(),
            textChannel.getTitle(),
            textChannel.getOwner().getId(),
            textChannel.getChatRoom().getId()
        );
    }

    public TextChannelResponse(String id, String title, String ownerId, String chatRoomId) {
        this.id = id;
        this.title = title;
        this.ownerId = ownerId;
        this.chatRoomId = chatRoomId;
    }
}

