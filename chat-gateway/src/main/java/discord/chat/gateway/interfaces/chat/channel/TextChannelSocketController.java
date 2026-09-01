package discord.chat.gateway.interfaces.chat.channel;

import java.time.Instant;
import java.util.UUID;

import discord.chat.common.infrastructure.user.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class TextChannelSocketController {
    private static final Logger logger = LoggerFactory.getLogger(TextChannelSocketController.class);
    private static final String CHANNEL_DESTINATION_PREFIX = "/channel/";

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/sendText")
    public void handleTextMessage(SendTextMessageRequest request, Authentication authentication) {
        User sender = (User) authentication.getPrincipal();

        logger.info(
            "WebSocket message received: user={}, chatRoomId={}, textChannelId={}, content={}",
            sender.getId(),
            request.getChatRoomId(),
            request.getTextChannelId(),
            request.getContent()
        );

        ReceivedTextMessageResponse response = new ReceivedTextMessageResponse(
            UUID.randomUUID().toString(),
            request.getChatRoomId(),
            request.getTextChannelId(),
            new MessageSenderResponse(sender.getId(), sender.getNickName(), sender.getImageUrl()),
            request.getContent(),
            Instant.now()
        );

        messagingTemplate.convertAndSend(
            CHANNEL_DESTINATION_PREFIX + request.getTextChannelId(),
            response
        );
    }
}
