package discord.chat.message.domain.message;

import discord.chat.common.exception.CustomIllegalArgumentException;
import discord.chat.message.infrastructure.message.ChatMessage;
import discord.chat.message.infrastructure.message.ChatMessageMongoRepository;
import discord.chat.message.interfaces.message.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageHistoryService {
    private final ChatMessageMongoRepository chatMessageMongoRepository;

    public List<MessageResponse> getMessages(
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
        if (beforeMessageId != null && !ObjectId.isValid(beforeMessageId)) {
            throw new CustomIllegalArgumentException(
                "INVALID_MESSAGE_CURSOR",
                "Message cursor must be a valid ObjectId"
            );
        }

        PageRequest page = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "id"));
        List<ChatMessage> messages = beforeMessageId == null
            ? chatMessageMongoRepository.findByChatRoomIdAndTextChannelId(
                chatRoomId,
                textChannelId,
                page
            )
            : chatMessageMongoRepository.findMessagesBefore(
                chatRoomId,
                textChannelId,
                new ObjectId(beforeMessageId),
                page
            );

        return messages.stream().map(MessageResponse::new).toList();
    }
}
