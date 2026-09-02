package discord.chat.message.application.message;

import discord.chat.common.infrastructure.user.User;
import discord.chat.message.domain.message.MessageCreationService;
import discord.chat.message.infrastructure.websocket.WebSocketSessionAuthorizationRegistry;
import discord.chat.message.interfaces.message.MessageResponse;
import discord.chat.message.interfaces.message.MessageSenderResponse;
import discord.chat.message.interfaces.message.ReceivedTextMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RealtimeMessageService {
    private static final String PERSONAL_CHANNEL_DESTINATION = "/channel";

    private final WebSocketSessionAuthorizationRegistry sessionAuthorizationRegistry;
    private final MessageCreationService messageCreationService;
    private final SimpMessagingTemplate messagingTemplate;

    public void send(
        String chatRoomId,
        String textChannelId,
        String content,
        Authentication authentication,
        String sessionId
    ) {
        User sender = (User) authentication.getPrincipal();
        String authorizedChatRoomId = sessionAuthorizationRegistry
            .getAuthorizedChatRoomId(sessionId, textChannelId)
            .orElseThrow(() -> new AccessDeniedException("No access to text channel"));

        if (!authorizedChatRoomId.equals(chatRoomId)) {
            throw new AccessDeniedException("Text channel does not belong to chat room");
        }

        MessageResponse storedMessage = messageCreationService.create(
            authorizedChatRoomId,
            textChannelId,
            sender.getId(),
            content
        );

        ReceivedTextMessageResponse response = new ReceivedTextMessageResponse(
            storedMessage.getMessageId(),
            storedMessage.getChatRoomId(),
            storedMessage.getTextChannelId(),
            new MessageSenderResponse(sender.getId(), sender.getNickName(), sender.getImageUrl()),
            storedMessage.getContent(),
            storedMessage.getCreatedAt()
        );

        messagingTemplate.convertAndSendToUser(
            sender.getId(),
            PERSONAL_CHANNEL_DESTINATION,
            response
        );
    }
}
