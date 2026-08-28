package discord.chat.common.infrastructure.chat.room;

import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import discord.chat.common.infrastructure.user.User;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Document(collection = "chat_rooms")
@NoArgsConstructor
public class ChatRoom {
    @Id
    private String id;

    @Setter
    private String title;

    @Setter
    private String image;

    private ChatRoomType type;

    @DBRef
    private User owner;

    public ChatRoom(User owner, String title, String image, ChatRoomType type) {
        this.owner = owner;
        this.title = title;
        this.image = image;
        this.type = type;
    }
}
