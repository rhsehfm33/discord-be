package discord.chat.gateway.infrastructure.chat.channel;

import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import discord.chat.gateway.infrastructure.chat.room.ChatRoom;
import discord.chat.gateway.infrastructure.user.User;

@Getter
@NoArgsConstructor
@Document(collection = "text_channels")
public class TextChannel {
    @Id
    private String id;

    @Setter
    private String title;

    @DBRef
    private User owner;

    @DBRef
    private ChatRoom chatRoom;

    public TextChannel(String title, User owner, ChatRoom chatRoom) {
        this.title = title;
        this.owner = owner;
        this.chatRoom = chatRoom;
    }
}
