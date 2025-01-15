package discord.chat.gateway.interfaces.common.listener;

import java.security.Principal;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleWebSocketConnectedEvent(SessionConnectedEvent event) {
        logger.info("Connected websocket user:{}", Objects.requireNonNull(event.getUser()));
    }

    @EventListener
    public void handleSubscriptionEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String subscribedPath = headerAccessor.getDestination();
        Principal user = headerAccessor.getUser();

        if (subscribedPath != null && user != null && subscribedPath.equals("/user/channel")) {
            String welcomeMessage = "User[" + user.getName() + "] has subscribed channel!";
            // 사용자에게 초기 메시지 전송
            messagingTemplate.convertAndSendToUser(user.getName(), "/channel", welcomeMessage);
            logger.info(welcomeMessage);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        logger.info("Disconnected websocket user: {}", Objects.requireNonNull(event.getUser()));
    }
}