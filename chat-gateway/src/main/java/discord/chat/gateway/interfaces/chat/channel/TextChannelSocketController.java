package discord.chat.gateway.interfaces.chat.channel;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class TextChannelSocketController {
    private static final Logger logger = LoggerFactory.getLogger(TextChannelSocketController.class);

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/sendText")
    public void handleTextMessage(String textMessage, Principal principal) {
        logger.info("user[{}] text Message:{}", principal.getName(), textMessage);

        // WebSocket 메시지 처리 로직
        String destination = "/channel";
        messagingTemplate.convertAndSendToUser(principal.getName(), destination, textMessage);
    }
}
