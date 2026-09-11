package discord.chat.api.domain.message;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import discord.chat.api.infrastructure.message.ChatMessage;
import discord.chat.api.infrastructure.message.ChatMessageRepository;
import discord.chat.common.exception.CustomIllegalArgumentException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageHistoryService {
    private final ChatMessageRepository chatMessageRepository;

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
        if (beforeMessageId != null && !ObjectId.isValid(beforeMessageId)) {
            throw new CustomIllegalArgumentException(
                "INVALID_MESSAGE_CURSOR",
                "Message cursor must be a valid ObjectId"
            );
        }

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
}
