package discord.chat.gateway.interfaces.chat.channel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MessageSenderResponse {
    private final String id;
    private final String nickName;
    private final String imageUrl;
}
