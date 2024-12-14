package team.discordbe.interfaces.chat.room;

import lombok.Getter;
import lombok.Setter;
import team.discordbe.domain.chat.room.ChatRoomType;

@Getter
public class ChatRoomRequest {
    private String id;
    private String title;
    private ChatRoomType type;

    @Setter
    private String image;
}
