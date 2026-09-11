package discord.chat.api.application.message;

import discord.chat.api.infrastructure.websocket.ChatSessionRegistry;
import discord.chat.api.infrastructure.websocket.WebSocketSessionMessageSender;
import discord.chat.api.interfaces.message.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomMessageDelivery {
    private final ChatSessionRegistry sessions;
    private final WebSocketSessionMessageSender sessionMessageSender;

    @EventListener
    public void deliver(ChatMessageResponse message) {
        for (String sessionId : sessions.getSessionIds(message.getChatRoomId())) {
            try {
                sessionMessageSender.send(sessionId, "/channel", message);
            } catch (RuntimeException exception) {
                log.error(
                    "Local message delivery failed: messageId={}, sessionId={}",
                    message.getMessageId(),
                    sessionId,
                    exception
                );
            }
        }
    }
}
