package team.discordbe.domain.chat.room.dto;

import lombok.Getter;
import team.discordbe.domain.chat.room.constant.ChatRoomType;

@Getter
public class ChatRoomRequestDto {
    private String id;
    private String title;
    private ChatRoomType type;
}
