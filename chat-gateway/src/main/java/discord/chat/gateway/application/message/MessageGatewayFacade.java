package discord.chat.gateway.application.message;

import discord.chat.common.infrastructure.user.User;
import discord.chat.gateway.infrastructure.client.message.ChatMessageClient;
import discord.chat.gateway.infrastructure.client.message.StoredMessageResponse;
import discord.chat.gateway.infrastructure.websocket.WebSocketSessionAuthorizationRegistry;
import discord.chat.gateway.interfaces.chat.channel.MessageSenderResponse;
import discord.chat.gateway.interfaces.chat.channel.ReceivedTextMessageResponse;
import discord.chat.gateway.interfaces.chat.channel.SendTextMessageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageGatewayFacade {
    private static final String PERSONAL_CHANNEL_DESTINATION = "/channel";

    private final WebSocketSessionAuthorizationRegistry sessionAuthorizationRegistry;
    private final ChatMessageClient chatMessageClient;
    private final SimpMessagingTemplate messagingTemplate;

    public void send(
        SendTextMessageRequest request,
        Authentication authentication,
        String sessionId
    ) {
        User sender = (User) authentication.getPrincipal();
        String authorizedChatRoomId = sessionAuthorizationRegistry
            .getAuthorizedChatRoomId(sessionId, request.getTextChannelId())
            .orElseThrow(() -> new AccessDeniedException("No access to text channel"));

        if (!authorizedChatRoomId.equals(request.getChatRoomId())) {
            throw new AccessDeniedException("Text channel does not belong to chat room");
        }

        StoredMessageResponse storedMessage = chatMessageClient.createMessage(
            authorizedChatRoomId,
            request.getTextChannelId(),
            sender.getId(),
            request.getContent()
        );

        ReceivedTextMessageResponse response = new ReceivedTextMessageResponse(
            storedMessage.messageId(),
            storedMessage.chatRoomId(),
            storedMessage.textChannelId(),
            new MessageSenderResponse(sender.getId(), sender.getNickName(), sender.getImageUrl()),
            storedMessage.content(),
            storedMessage.createdAt()
        );

        messagingTemplate.convertAndSendToUser(
            sender.getId(),
            PERSONAL_CHANNEL_DESTINATION,
            response
        );
    }
}
