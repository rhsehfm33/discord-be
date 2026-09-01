package discord.chat.message.infrastructure.message;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Document(collection = "chat_messages")
public class ChatMessage {
    @Id
    private String id;

    @Field("sender_id")
    private String senderId;

    @Field("chat_room_id")
    private String chatRoomId;

    @Field("text_channel_id")
    private String textChannelId;

    @Field("content")
    private String content;

    @Field("created_at")
    private Instant createdAt;

    public ChatMessage(
        String senderId,
        String chatRoomId,
        String textChannelId,
        String content
    ) {
        this.senderId = senderId;
        this.chatRoomId = chatRoomId;
        this.textChannelId = textChannelId;
        this.content = content;
        this.createdAt = Instant.now();
    }
}
