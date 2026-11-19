package discord.chat.api.application.session;

import discord.chat.api.infrastructure.websocket.ChatSessionRegistry;
import discord.chat.api.domain.chat.channel.TextChannelCache;
import discord.chat.api.infrastructure.websocket.WebSocketUserMessageSender;
import discord.chat.api.interfaces.chat.channel.TextChannelResponse;
import discord.chat.api.interfaces.chat.room.ChatRoomEventResponse;
import discord.chat.api.interfaces.chat.room.ChatRoomParticipantResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatSessionEventHandler {
    private static final String CHAT_ROOM_EVENT_DESTINATION = "/channel/events";

    private final ChatSessionRegistry chatSessionRegistry;
    private final TextChannelCache textChannelCache;
    private final WebSocketUserMessageSender webSocketUserMessageSender;

    public record PersonalJoinReceived(String userId, String chatRoomId) {}

    @EventListener
    public void handleRoomEvent(ChatSessionEvent event) {
        switch (event.type()) {
            case JOIN -> join(event);
            case LEAVE -> leave(event.userId(), event.nickName(), event.chatRoomId());
            case DELETE -> delete(event.chatRoomId());
        }
    }

    @EventListener
    public void handleTextChannelEvent(TextChannelEvent event) {
        if (event.type() == TextChannelEvent.Type.TEXT_CHANNEL_DELETED) {
            textChannelCache.invalidate(event.textChannelId());
        }
        TextChannelResponse textChannel = new TextChannelResponse(
            event.textChannelId(), event.title(), event.ownerId(), event.chatRoomId()
        );
        ChatRoomEventResponse.Type responseType = switch (event.type()) {
            case TEXT_CHANNEL_CREATED -> ChatRoomEventResponse.Type.TEXT_CHANNEL_CREATED;
            case TEXT_CHANNEL_UPDATED -> ChatRoomEventResponse.Type.TEXT_CHANNEL_UPDATED;
            case TEXT_CHANNEL_DELETED -> ChatRoomEventResponse.Type.TEXT_CHANNEL_DELETED;
        };
        spreadChatRoomEvent(event.chatRoomId(), ChatRoomEventResponse.textChannelChanged(responseType, textChannel));
    }

    @EventListener
    public void join(PersonalJoinReceived event) {
        chatSessionRegistry.join(event.userId(), event.chatRoomId());
    }

    private void join(ChatSessionEvent event) {
        ChatRoomParticipantResponse participant = ChatRoomParticipantResponse.builder()
            .id(event.userId())
            .nickName(event.nickName())
            .imageUrl(event.imageUrl())
            .build();
        ChatRoomEventResponse response = ChatRoomEventResponse.subscribed(event.chatRoomId(), participant);
        spreadChatRoomEvent(event.chatRoomId(), response);
    }

    private void leave(String userId, String nickName, String chatRoomId) {
        chatSessionRegistry.leave(userId, chatRoomId);
        ChatRoomParticipantResponse participant = ChatRoomParticipantResponse.builder()
            .id(userId)
            .nickName(nickName)
            .build();
        spreadChatRoomEvent(chatRoomId, ChatRoomEventResponse.unsubscribed(chatRoomId, participant));
    }

    private void delete(String chatRoomId) {
        ChatRoomEventResponse response = ChatRoomEventResponse.deleted(chatRoomId);
        for (String userId : chatSessionRegistry.getUserIds(chatRoomId)) {
            send(userId, response);
        }
        chatSessionRegistry.delete(chatRoomId);
    }

    private void spreadChatRoomEvent(String chatRoomId, ChatRoomEventResponse response) {
        for (String userId : chatSessionRegistry.getUserIds(chatRoomId)) {
            send(userId, response);
        }
    }

    private void send(String userId, ChatRoomEventResponse response) {
        try {
            webSocketUserMessageSender.send(userId, CHAT_ROOM_EVENT_DESTINATION, response);
        } catch (RuntimeException exception) {
            log.error(
                "Chat room event delivery failed: userId={}, chatRoomId={}",
                userId,
                response.chatRoomId(),
                exception
            );
        }
    }
}
