package team.discordbe.infrastructure.chat.room;

import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import team.discordbe.domain.chat.room.ChatRoomType;
import team.discordbe.infrastructure.user.User;
import team.discordbe.interfaces.chat.room.ChatRoomRequest;

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

    public ChatRoom(User owner, ChatRoomRequest chatRoomRequest) {
        this.owner = owner;
        this.title = chatRoomRequest.getTitle();
        this.image = chatRoomRequest.getImage();
        this.type = chatRoomRequest.getType();
    }
}
