package discord.chat.api.application.session;

import discord.chat.api.domain.chat.channel.ChannelAccessService;
import discord.chat.api.infrastructure.websocket.ChatSessionRegistry;
import discord.chat.api.infrastructure.websocket.WebSocketUserMessageSender;
import discord.chat.api.interfaces.chat.room.ChatRoomEventResponse;
import discord.chat.api.interfaces.chat.room.ChatRoomParticipantResponse;
import discord.chat.common.infrastructure.chat.channel.TextChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatSessionEventHandler {
    private static final String CHAT_ROOM_EVENT_DESTINATION = "/channel/events";

    private final ChannelAccessService channelAccessService;
    private final ChatSessionRegistry chatSessionRegistry;
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
    public void join(PersonalJoinReceived event) {
        String userId = event.userId();
        if (chatSessionRegistry.hasUserSessions(userId)) {
            List<TextChannel> textChannels = channelAccessService.getTextChannelsBy(userId, event.chatRoomId());
            chatSessionRegistry.join(userId, textChannels);
        }
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
