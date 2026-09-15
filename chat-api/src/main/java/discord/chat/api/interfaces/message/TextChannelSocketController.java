package discord.chat.api.interfaces.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import discord.chat.api.application.message.ChatMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class TextChannelSocketController {
    private static final Logger logger = LoggerFactory.getLogger(TextChannelSocketController.class);

    private final ChatMessageService chatMessageService;

    @MessageMapping("/sendText")
    public void handleTextMessage(
        @Valid ChatMessageRequest request,
        Authentication authentication
    ) {
        logger.info(
            "WebSocket message received: user={}, chatRoomId={}, textChannelId={}",
            authentication.getName(),
            request.getChatRoomId(),
            request.getTextChannelId()
        );
        chatMessageService.send(
            request.getChatRoomId(),
            request.getTextChannelId(),
            request.getContent(),
            authentication
        );
    }
}
