package discord.chat.message.domain.message;

import org.springframework.stereotype.Service;

import discord.chat.message.infrastructure.message.ChatMessage;
import discord.chat.message.infrastructure.message.ChatMessageMongoRepository;
import discord.chat.message.interfaces.message.MessageResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageCreationService {
    private final ChatMessageMongoRepository chatMessageMongoRepository;

    public MessageResponse create(
        String chatRoomId,
        String textChannelId,
        String senderId,
        String content
    ) {
        ChatMessage message = chatMessageMongoRepository.save(
            new ChatMessage(
                senderId,
                chatRoomId,
                textChannelId,
                content
            )
        );

        return new MessageResponse(message);
    }
}
