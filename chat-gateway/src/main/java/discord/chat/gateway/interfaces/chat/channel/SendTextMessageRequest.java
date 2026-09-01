package discord.chat.gateway.interfaces.chat.channel;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SendTextMessageRequest {
    private String chatRoomId;
    private String textChannelId;
    private String content;
}
