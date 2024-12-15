package discord.chat.api.interfaces.chat.room;

import discord.chat.api.domain.chat.room.ChatRoomType;
import lombok.Getter;
import lombok.Setter;

@Getter
public class ChatRoomRequest {
    private String id;
    private String title;
    private ChatRoomType type;

    @Setter
    private String image;
}
