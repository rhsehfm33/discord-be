package discord.chat.common.infrastructure.chat.message;

import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import discord.chat.common.infrastructure.chat.channel.TextChannel;
import discord.chat.common.infrastructure.chat.room.ChatRoom;
import discord.chat.common.infrastructure.common.BaseEntity;
import discord.chat.common.infrastructure.user.User;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
