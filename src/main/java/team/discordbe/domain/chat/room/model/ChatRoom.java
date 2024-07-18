package team.discordbe.domain.chat.room.model;

import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import team.discordbe.domain.chat.room.constant.ChatRoomType;
import team.discordbe.domain.chat.room.dto.ChatRoomRequestDto;
import team.discordbe.domain.user.model.User;

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

    public ChatRoom(User owner, ChatRoomRequestDto chatRoomRequestDto) {
        this.owner = owner;
        this.title = chatRoomRequestDto.getTitle();
        this.image = chatRoomRequestDto.getImage();
        this.type = chatRoomRequestDto.getType();
    }
}
