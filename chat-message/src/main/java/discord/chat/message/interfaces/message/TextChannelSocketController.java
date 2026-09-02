package discord.chat.message.interfaces.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import discord.chat.message.application.message.RealtimeMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class TextChannelSocketController {
    private static final Logger logger = LoggerFactory.getLogger(TextChannelSocketController.class);

    private final RealtimeMessageService realtimeMessageService;

    @MessageMapping("/sendText")
    public void handleTextMessage(
        @Valid SendTextMessageRequest request,
        Authentication authentication,
        @Header("simpSessionId") String sessionId
    ) {
        logger.info(
            "WebSocket message received: user={}, chatRoomId={}, textChannelId={}",
            authentication.getName(),
            request.getChatRoomId(),
            request.getTextChannelId()
        );
        realtimeMessageService.send(
            request.getChatRoomId(),
            request.getTextChannelId(),
            request.getContent(),
            authentication,
            sessionId
        );
    }
}
