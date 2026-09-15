package discord.chat.api.application.message;

import discord.chat.api.infrastructure.websocket.ChatSessionRegistry;
import discord.chat.api.infrastructure.websocket.WebSocketUserMessageSender;
import discord.chat.api.interfaces.message.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomMessageDelivery {
    private final ChatSessionRegistry chatSessionRegistry;
    private final WebSocketUserMessageSender webSocketUserMessageSender;

    @EventListener
    public void deliver(ChatMessageResponse message) {
        for (String userId : chatSessionRegistry.getUserIds(message.getChatRoomId())) {
            try {
                webSocketUserMessageSender.send(userId, "/channel", message);
            } catch (RuntimeException exception) {
                log.error(
                    "Local message delivery failed: messageId={}, userId={}",
                    message.getMessageId(),
                    userId,
                    exception
                );
            }
        }
    }
}
