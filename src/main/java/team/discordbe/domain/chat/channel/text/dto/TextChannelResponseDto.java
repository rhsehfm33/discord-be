package team.discordbe.domain.chat.channel.text.dto;

import lombok.Getter;
import team.discordbe.domain.chat.channel.text.model.TextChannel;

@Getter
public class TextChannelResponseDto {
    private final String id;
    private final String title;
    private final String ownerId;
    private final String chatRoomId;

    public TextChannelResponseDto(TextChannel textChannel) {
        this.id = textChannel.getId();
        this.title = textChannel.getTitle();
        this.ownerId = textChannel.getOwner().getId();
        this.chatRoomId = textChannel.getChatRoom().getId();
    }
}
