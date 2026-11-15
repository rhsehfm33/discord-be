package discord.chat.api.interfaces.directmessage;

import java.time.Instant;

import discord.chat.api.interfaces.message.MessageSenderResponse;

public record DirectMessageMessageResponse(
    String messageId,
    String directMessageId,
    MessageSenderResponse sender,
    String content,
    Instant createdAt
) {
}
