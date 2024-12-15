package discord.chat.gateway.interfaces.chat.channel;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TextChannelRequest {
    private String id;
    private String title;
    private String chatRoomId;
}
