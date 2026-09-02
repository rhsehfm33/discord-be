package discord.chat.message.interfaces.message;

import java.time.Instant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ReceivedTextMessageResponse {
    private final String messageId;
    private final String chatRoomId;
    private final String textChannelId;
    private final MessageSenderResponse sender;
    private final String content;
    private final Instant createdAt;
}
