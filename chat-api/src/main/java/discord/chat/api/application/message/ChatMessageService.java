package discord.chat.api.application.message;

import discord.chat.api.infrastructure.message.ChatMessage;
import discord.chat.api.infrastructure.message.ChatMessageRepository;
import discord.chat.api.infrastructure.redis.ChatMessageRedisBroker;
import discord.chat.api.infrastructure.redis.DirectMessageRedisBroker;
import discord.chat.api.infrastructure.websocket.ChatSessionRegistry;
import discord.chat.api.infrastructure.websocket.WebSocketUserMessageSender;
import discord.chat.api.interfaces.directmessage.DirectMessageMessageResponse;
import discord.chat.api.interfaces.message.ChatMessageResponse;
import discord.chat.api.interfaces.message.MessagePublishErrorResponse;
import discord.chat.api.interfaces.message.MessageSenderResponse;
import discord.chat.common.exception.CustomEntityNotFoundException;
import discord.chat.common.exception.CustomIllegalArgumentException;
import discord.chat.common.infrastructure.chat.directmessage.DirectMessage;
import discord.chat.common.infrastructure.chat.directmessage.DirectMessageMongoRepository;
import discord.chat.common.infrastructure.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatMessageService {
    private static final int DIRECT_MESSAGE_PAGE_SIZE = 50;

    private final ChatSessionRegistry chatSessionRegistry;
    private final ChatMessageRepository chatMessageRepository;
    private final WebSocketUserMessageSender webSocketUserMessageSender;
    private final ChatMessageRedisBroker chatMessageRedisBroker;
    private final DirectMessageRedisBroker directMessageRedisBroker;
    private final DirectMessageMongoRepository directMessageMongoRepository;

    public void send(
        String chatRoomId,
        String textChannelId,
        String content,
        Authentication authentication
    ) {
        User sender = (User) authentication.getPrincipal();
        boolean hasAccess = chatSessionRegistry.hasChannelAccess(
            sender.getId(),
            chatRoomId,
            textChannelId
        );
        if (!hasAccess) {
            throw new AccessDeniedException("No access to text channel");
        }

        ChatMessage message = new ChatMessage(
            sender.getId(), chatRoomId, textChannelId, content
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
            chatMessageRedisBroker.publish(response);
        } catch (Exception exception) {
            log.error("Message stored but Redis publication failed: messageId={}",
                storedMessage.getId(), exception);
            MessagePublishErrorResponse error = new MessagePublishErrorResponse(
                "MESSAGE_PUBLISH_FAILED", storedMessage.getId(), chatRoomId,
                textChannelId, "메시지는 저장됐지만 실시간 전달에 실패했습니다."
            );
            webSocketUserMessageSender.send(sender.getId(), "/channel/errors", error);
        }
    }

    public List<ChatMessage> getMessages(
        String chatRoomId,
        String textChannelId,
        String beforeMessageId,
        int limit
    ) throws CustomIllegalArgumentException {
        if (limit < 1 || limit > 100) {
            throw new CustomIllegalArgumentException(
                "INVALID_MESSAGE_LIMIT",
                "Message limit must be between 1 and 100"
            );
        }
        validateCursor(beforeMessageId);

        PageRequest page = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "id"));
        if (beforeMessageId == null) {
            return chatMessageRepository.findByChatRoomIdAndTextChannelId(
                chatRoomId,
                textChannelId,
                page
            );
        }

        return chatMessageRepository.findMessagesBefore(
            chatRoomId,
            textChannelId,
            new ObjectId(beforeMessageId),
            page
        );
    }

    public List<ChatMessage> getDirectMessageHistory(String userId, String directMessageId, String beforeMessageId)
        throws CustomEntityNotFoundException, CustomIllegalArgumentException {
        requireDirectMessageParticipant(userId, directMessageId);
        validateCursor(beforeMessageId);
        PageRequest page = PageRequest.of(0, DIRECT_MESSAGE_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id"));
        if (beforeMessageId == null) {
            return chatMessageRepository.findByDirectMessageId(directMessageId, page);
        }
        return chatMessageRepository.findDirectMessagesBefore(
            directMessageId, new ObjectId(beforeMessageId), page
        );
    }

    public boolean sendDirectMessage(
        Authentication authentication, String directMessageId, String content
    ) throws CustomEntityNotFoundException {
        User sender = (User) authentication.getPrincipal();
        DirectMessage directMessage = requireDirectMessageParticipant(sender.getId(), directMessageId);

        ChatMessage storedMessage;
        try {
            storedMessage = chatMessageRepository.save(new ChatMessage(sender.getId(), directMessageId, content));
        } catch (DataAccessException exception) {
            log.error("Direct message could not be saved: directMessageId={}", directMessageId, exception);
            return false;
        }
        MessageSenderResponse messageSender = new MessageSenderResponse(
            sender.getId(), sender.getNickName(), sender.getImageUrl()
        );
        DirectMessageMessageResponse response = new DirectMessageMessageResponse(
            storedMessage.getId(), directMessageId, messageSender, storedMessage.getContent(),
            storedMessage.getCreatedAt()
        );
        directMessageRedisBroker.publish(response, directMessage.getParticipantIds());
        return true;
    }

    private DirectMessage requireDirectMessageParticipant(String userId, String directMessageId)
        throws CustomEntityNotFoundException {
        return directMessageMongoRepository.findById(directMessageId)
            .filter(directMessage -> directMessage.hasParticipant(userId))
            .orElseThrow(() -> new CustomEntityNotFoundException("NOT_FOUND", "Direct message not found"));
    }

    private void validateCursor(String beforeMessageId) throws CustomIllegalArgumentException {
        if (beforeMessageId != null && !ObjectId.isValid(beforeMessageId)) {
            throw new CustomIllegalArgumentException(
                "INVALID_MESSAGE_CURSOR",
                "Message cursor must be a valid ObjectId"
            );
        }
    }
}
