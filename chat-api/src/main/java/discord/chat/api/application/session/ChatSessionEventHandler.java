package discord.chat.api.application.session;

import discord.chat.api.domain.chat.channel.ChannelAccessService;
import discord.chat.api.infrastructure.websocket.ChatSessionRegistry;
import discord.chat.api.infrastructure.websocket.WebSocketUserMessageSender;
import discord.chat.api.interfaces.chat.room.ChatRoomEventResponse;
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

    @EventListener
    public void handle(ChatSessionEvent event) {
        switch (event.type()) {
            case JOIN -> join(event.userId(), event.chatRoomId());
            case LEAVE -> chatSessionRegistry.leave(event.userId(), event.chatRoomId());
            case DELETE -> delete(event.chatRoomId());
        }
    }

    private void join(String userId, String chatRoomId) {
        if (!chatSessionRegistry.hasUserSessions(userId)) {
            return;
        }

        List<TextChannel> textChannels = channelAccessService.getTextChannelsBy(userId, chatRoomId);
        chatSessionRegistry.join(userId, textChannels);
    }

    private void delete(String chatRoomId) {
        ChatRoomEventResponse response = ChatRoomEventResponse.deleted(chatRoomId);
        for (String userId : chatSessionRegistry.getUserIds(chatRoomId)) {
            send(userId, response);
        }
        chatSessionRegistry.delete(chatRoomId);
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
