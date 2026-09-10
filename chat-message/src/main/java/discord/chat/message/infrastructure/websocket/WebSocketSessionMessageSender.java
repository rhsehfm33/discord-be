package discord.chat.message.infrastructure.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketSessionMessageSender {
    private final SimpMessagingTemplate messagingTemplate;

    public void send(String sessionId, String destination, Object message) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setSessionId(sessionId);
        headers.setLeaveMutable(true);
        messagingTemplate.convertAndSendToUser(
            sessionId,
            destination,
            message,
            headers.getMessageHeaders()
        );
    }
}
