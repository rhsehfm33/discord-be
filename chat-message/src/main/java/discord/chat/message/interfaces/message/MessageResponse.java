package discord.chat.message.interfaces.message;

import java.time.Instant;

import discord.chat.message.infrastructure.message.ChatMessage;
import lombok.Getter;

@Getter
public class MessageResponse {
    private final String messageId;
    private final String senderId;
    private final String chatRoomId;
    private final String textChannelId;
    private final String content;
    private final Instant createdAt;

    public MessageResponse(ChatMessage message) {
        this.messageId = message.getId();
        this.senderId = message.getSenderId();
        this.chatRoomId = message.getChatRoomId();
        this.textChannelId = message.getTextChannelId();
        this.content = message.getContent();
        this.createdAt = message.getCreatedAt();
    }
}
