package discord.chat.api.application.message;

import discord.chat.api.infrastructure.websocket.WebSocketUserMessageSender;
import discord.chat.api.interfaces.directmessage.DirectMessageMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DirectMessageDelivery {
    private final WebSocketUserMessageSender webSocketUserMessageSender;

    public record DirectMessageReceived(String userId, DirectMessageMessageResponse message) {
    }

    @EventListener
    public void deliver(DirectMessageReceived event) {
        webSocketUserMessageSender.send(event.userId(), "/channel/direct-messages", event.message());
    }

}
