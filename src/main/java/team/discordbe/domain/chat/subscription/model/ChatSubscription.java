package team.discordbe.domain.chat.subscription.model;

import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.discordbe.domain.chat.room.model.ChatRoom;
import team.discordbe.domain.user.model.User;

@Getter
@Document(collection = "chat_subscriptions")
@NoArgsConstructor
public class ChatSubscription {
    @Id
    private String id;

    @DBRef
    private User user;

    @DBRef
    private ChatRoom chatRoom;

    public ChatSubscription(User user, ChatRoom chatRoom) {
        this.user = user;
        this.chatRoom = chatRoom;
    }
}
