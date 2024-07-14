package team.discordbe.domain.chat.channel.text.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TextChannelRequestDto {
    private String id;
    private String title;
    private String chatRoomId;
}
