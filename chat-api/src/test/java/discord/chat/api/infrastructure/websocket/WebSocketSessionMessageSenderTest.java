package discord.chat.api.infrastructure.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WebSocketUserMessageSenderTest {
    // Checks that the outgoing message targets every local session owned by the user.
    @Test
    void sendToUserDestination() {
        SimpMessagingTemplate simpMessagingTemplate = mock(SimpMessagingTemplate.class);
        var webSocketUserMessageSender = new WebSocketUserMessageSender(simpMessagingTemplate);

        webSocketUserMessageSender.send("user", "/channel/errors", "error");

        verify(simpMessagingTemplate).convertAndSendToUser("user", "/channel/errors", "error");
    }
}
