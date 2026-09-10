package discord.chat.message.test.unit;

import discord.chat.message.infrastructure.websocket.WebSocketSessionMessageSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WebSocketSessionMessageSenderTest {
    // Checks that the outgoing message targets the given session.
    @Test
    void includeSessionIdInDeliveryHeaders() {
        SimpMessagingTemplate simpMessagingTemplate = mock(SimpMessagingTemplate.class);
        var webSocketSessionMessageSender = new WebSocketSessionMessageSender(simpMessagingTemplate);

        webSocketSessionMessageSender.send("session", "/channel/errors", "error");

        ArgumentCaptor<MessageHeaders> messageHeadersCaptor = ArgumentCaptor.forClass(MessageHeaders.class);
        verify(simpMessagingTemplate).convertAndSendToUser(
            eq("session"), eq("/channel/errors"), eq("error"), messageHeadersCaptor.capture()
        );
        assertThat(messageHeadersCaptor.getValue().get("simpSessionId")).isEqualTo("session");
    }
}
