package discord.chat.common.infrastructure.chat.subsription;

import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import discord.chat.common.infrastructure.chat.room.ChatRoom;
import discord.chat.common.infrastructure.user.User;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
