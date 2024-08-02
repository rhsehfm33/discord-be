package team.discordbe.domain.chat.room.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import team.discordbe.domain.chat.room.constant.ChatRoomType;
import team.discordbe.domain.chat.room.model.ChatRoom;

@Getter
public class ChatRoomResponseDto {
    private final String id;
    private final String title;
    private final String image;
    private final ChatRoomType type;

    @JsonProperty("isMine")
    private final boolean isMine;

    public ChatRoomResponseDto(ChatRoom room, boolean isMine) {
        this.id = room.getId();
        this.title = room.getTitle();
        this.image = room.getImage();
        this.type = room.getType();
        this.isMine = isMine;
    }
}
