package discord.chat.gateway.interfaces.chat.room;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import discord.chat.common.infrastructure.chat.room.ChatRoomType;
import discord.chat.common.infrastructure.chat.room.ChatRoom;

@Getter
public class ChatRoomResponse {
    private final String id;
    private final String title;
    private final String image;
    private final ChatRoomType type;

    @JsonProperty("isMine")
    private final boolean isMine;

    public ChatRoomResponse(ChatRoom room, boolean isMine) {
        this.id = room.getId();
        this.title = room.getTitle();
        this.image = room.getImage();
        this.type = room.getType();
        this.isMine = isMine;
    }
}
