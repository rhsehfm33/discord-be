package discord.chat.gateway.infrastructure.chat.message;

import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import discord.chat.gateway.infrastructure.chat.channel.TextChannel;
import discord.chat.gateway.infrastructure.chat.room.ChatRoom;
import discord.chat.gateway.infrastructure.common.BaseEntity;
import discord.chat.gateway.infrastructure.user.User;

@Getter
@NoArgsConstructor
@Document(collection = "chat_messages")
public class ChatMessage extends BaseEntity {
    @Id
    private String id;

    @Setter
    private String message;

    @DBRef
    private User author;

    @DBRef
    private ChatRoom chatRoom;

    @DBRef
    private TextChannel textChannel;

    public ChatMessage(String message, User author, ChatRoom chatRoom, TextChannel textChannel) {
        this.message = message;
        this.author = author;
        this.chatRoom = chatRoom;
        this.textChannel = textChannel;
    }
}
