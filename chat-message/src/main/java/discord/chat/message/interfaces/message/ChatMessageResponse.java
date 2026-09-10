package discord.chat.message.interfaces.message;

import java.time.Instant;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@RequiredArgsConstructor
public class ChatMessageResponse {
    private final String messageId;
    private final String chatRoomId;
    private final String textChannelId;
    private final MessageSenderResponse sender;
    private final String content;
    private final Instant createdAt;
}
