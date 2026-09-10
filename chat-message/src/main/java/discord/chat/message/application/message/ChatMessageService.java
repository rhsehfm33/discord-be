package discord.chat.message.application.message;

import discord.chat.common.infrastructure.user.User;
import discord.chat.message.infrastructure.message.ChatMessage;
import discord.chat.message.infrastructure.message.ChatMessageRepository;
import discord.chat.message.infrastructure.redis.ChatMessageRedisBroker;
import discord.chat.message.infrastructure.websocket.ChatSessionRegistry;
import discord.chat.message.infrastructure.websocket.WebSocketSessionMessageSender;
import discord.chat.message.interfaces.message.MessagePublishErrorResponse;
import discord.chat.message.interfaces.message.MessageSenderResponse;
import discord.chat.message.interfaces.message.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatMessageService {
    private final ChatSessionRegistry chatSessionRegistry;
    private final ChatMessageRepository chatMessageRepository;
    private final WebSocketSessionMessageSender sessionMessageSender;
    private final ChatMessageRedisBroker redisBroker;

    public void send(
        String textChannelId,
        String content,
        Authentication authentication,
        String sessionId
    ) {
        User sender = (User) authentication.getPrincipal();
        String authorizedChatRoomId = chatSessionRegistry
            .getAuthorizedChatRoomId(sessionId, textChannelId)
            .orElseThrow(() -> new AccessDeniedException("No access to text channel"));

        ChatMessage message = new ChatMessage(
            sender.getId(), authorizedChatRoomId, textChannelId, content
        );
        ChatMessage storedMessage = chatMessageRepository.save(message);

        ChatMessageResponse response = new ChatMessageResponse(
            storedMessage.getId(),
            storedMessage.getChatRoomId(),
            storedMessage.getTextChannelId(),
            new MessageSenderResponse(sender.getId(), sender.getNickName(), sender.getImageUrl()),
            storedMessage.getContent(),
            storedMessage.getCreatedAt()
        );

        try {
            redisBroker.publish(response);
        } catch (Exception exception) {
            log.error("Message stored but Redis publication failed: messageId={}",
                storedMessage.getId(), exception);
            MessagePublishErrorResponse error = new MessagePublishErrorResponse(
                "MESSAGE_PUBLISH_FAILED", storedMessage.getId(), authorizedChatRoomId,
                textChannelId, "메시지는 저장됐지만 실시간 전달에 실패했습니다."
            );
            sessionMessageSender.send(sessionId, "/channel/errors", error);
        }
    }
}
