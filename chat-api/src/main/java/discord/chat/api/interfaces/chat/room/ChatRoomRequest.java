package discord.chat.api.interfaces.chat.room;

import lombok.Getter;
import lombok.Setter;
import discord.chat.common.infrastructure.chat.room.ChatRoomType;

@Getter
public class ChatRoomRequest {
    private String id;
    private String title;
    private ChatRoomType type;

    @Setter
    private String image;
}

