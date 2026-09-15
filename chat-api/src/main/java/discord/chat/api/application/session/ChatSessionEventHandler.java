package discord.chat.api.application.session;

import discord.chat.api.domain.chat.channel.ChannelAccessService;
import discord.chat.api.infrastructure.websocket.ChatSessionRegistry;
import discord.chat.common.infrastructure.chat.channel.TextChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatSessionEventHandler {
    private final ChannelAccessService channelAccessService;
    private final ChatSessionRegistry chatSessionRegistry;

    @EventListener
    public void handle(ChatSessionEvent event) {
        switch (event.type()) {
            case JOIN -> join(event.userId(), event.chatRoomId());
            case LEAVE -> chatSessionRegistry.leave(event.userId(), event.chatRoomId());
            case DELETE -> chatSessionRegistry.delete(event.chatRoomId());
        }
    }

    private void join(String userId, String chatRoomId) {
        if (!chatSessionRegistry.hasUserSessions(userId)) {
            return;
        }

        List<TextChannel> textChannels = channelAccessService.getTextChannelsBy(userId, chatRoomId);
        chatSessionRegistry.join(userId, textChannels);
    }
}
