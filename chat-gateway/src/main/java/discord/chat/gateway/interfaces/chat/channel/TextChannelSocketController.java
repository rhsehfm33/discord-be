package discord.chat.gateway.interfaces.chat.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import discord.chat.gateway.application.message.MessageGatewayFacade;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class TextChannelSocketController {
    private static final Logger logger = LoggerFactory.getLogger(TextChannelSocketController.class);

    private final MessageGatewayFacade messageGatewayFacade;

    @MessageMapping("/sendText")
    public void handleTextMessage(
        SendTextMessageRequest request,
        Authentication authentication,
        @Header("simpSessionId") String sessionId
    ) {
        logger.info(
            "WebSocket message received: user={}, chatRoomId={}, textChannelId={}",
            authentication.getName(),
            request.getChatRoomId(),
            request.getTextChannelId()
        );
        messageGatewayFacade.send(request, authentication, sessionId);
    }
}
