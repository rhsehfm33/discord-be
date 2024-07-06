package team.discordbe.domain.chat.room.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import team.discordbe.domain.chat.room.constant.ChatRoomType;

@Getter
@AllArgsConstructor
public class ChatRoomRequestDto {
    private String title;
    private ChatRoomType type;
}
