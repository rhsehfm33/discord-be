package team.discordbe.domain.chat.room.dto;

import lombok.Getter;
import team.discordbe.domain.chat.room.constant.ChatRoomType;
import team.discordbe.domain.chat.room.model.ChatRoom;

@Getter
public class ChatRoomResponseDto {
    private final String id;
    private final String title;
    private final ChatRoomType type;

    public ChatRoomResponseDto(ChatRoom room) {
        this.id = room.getId();
        this.title = room.getTitle();
        this.type = room.getType();
    }
}
