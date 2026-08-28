package discord.chat.gateway.interfaces.chat.channel;

import lombok.Getter;
import discord.chat.common.infrastructure.chat.channel.TextChannel;

@Getter
public class TextChannelResponse {
    private final String id;
    private final String title;
    private final String ownerId;
    private final String chatRoomId;

    public TextChannelResponse(TextChannel textChannel) {
        this.id = textChannel.getId();
        this.title = textChannel.getTitle();
        this.ownerId = textChannel.getOwner().getId();
        this.chatRoomId = textChannel.getChatRoom().getId();
    }
}
