package discord.chat.endpoint.domain.chat.channel;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import discord.chat.endpoint.infrastructure.chat.channel.TextChannel;
import discord.chat.endpoint.infrastructure.chat.channel.TextChannelMongoRepository;
import discord.chat.endpoint.infrastructure.chat.room.ChatRoom;
import discord.chat.endpoint.infrastructure.chat.room.ChatRoomMongoRepository;
import discord.chat.endpoint.infrastructure.chat.subsription.ChatSubscriptMongoRepository;
import discord.chat.endpoint.infrastructure.user.User;
import discord.chat.endpoint.interfaces.chat.channel.TextChannelRequest;
import discord.chat.endpoint.interfaces.chat.channel.TextChannelResponse;
import discord.chat.endpoint.interfaces.common.exception.CustomEntityNotFoundException;
import discord.chat.endpoint.interfaces.common.exception.CustomIllegalArgumentException;

@Service
@Transactional
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class TextChannelService {
    private final TextChannelMongoRepository textChannelMongoRepository;
    private final ChatRoomMongoRepository chatRoomMongoRepository;
    private final ChatSubscriptMongoRepository chatSubscriptMongoRepository;

    public TextChannelResponse create(
        Authentication authentication, TextChannelRequest textChannelRequest
    ) throws CustomEntityNotFoundException, CustomIllegalArgumentException {
        User owner = (User) authentication.getPrincipal();
        ChatRoom chatRoom = chatRoomMongoRepository.findById(textChannelRequest.getChatRoomId()).orElseThrow(
            () -> new CustomEntityNotFoundException("NOT_FOUND", "Chat room not found")
        );
        if (!chatRoom.getOwner().getId().equals(owner.getId())) {
            throw new CustomIllegalArgumentException("NO_AUTHORITY", "You are not the owner");
        }
        TextChannel textChannel = new TextChannel(textChannelRequest.getTitle(), owner, chatRoom);
        return new TextChannelResponse(textChannelMongoRepository.save(textChannel));
    }

    public List<TextChannelResponse> getAllByChatRoom(
        Authentication authentication, String chatRoomId
    ) throws CustomEntityNotFoundException {
        User owner = (User) authentication.getPrincipal();
        ChatRoom chatRoom = chatRoomMongoRepository.findById(chatRoomId).orElseThrow(
            () -> new CustomEntityNotFoundException("NOT_FOUND", "Chat room not found")
        );
        chatSubscriptMongoRepository.findByUserAndChatRoom(owner, chatRoom).orElseThrow(
            () -> new CustomEntityNotFoundException("NO_AUTHORITY", "You don't belong to this chat room")
        );

        List<TextChannelResponse> textChannelResponses = textChannelMongoRepository.findAllByChatRoom(chatRoom)
            .stream()
            .map(TextChannelResponse::new)
            .toList();
        return textChannelResponses;
    }

    public TextChannelResponse update(
        Authentication authentication, TextChannelRequest textChannelRequest
    ) throws CustomEntityNotFoundException, CustomIllegalArgumentException {
        User owner = (User) authentication.getPrincipal();
        TextChannel textChannel = textChannelMongoRepository.findById(textChannelRequest.getId()).orElseThrow(
            () -> new CustomEntityNotFoundException("NOT_FOUND", "Text channel not found")
        );
        if (!textChannel.getOwner().getId().equals(owner.getId())) {
            throw new CustomIllegalArgumentException("NO_AUTHORITY", "You are not the owner");
        }
        textChannel.setTitle(textChannelRequest.getTitle());
        return new TextChannelResponse(textChannelMongoRepository.save(textChannel));
    }

    public void delete(
        Authentication authentication, String textChannelId
    ) throws CustomEntityNotFoundException, CustomIllegalArgumentException {
        User owner = (User) authentication.getPrincipal();
        TextChannel textChannel = textChannelMongoRepository.findById(textChannelId).orElseThrow(
            () -> new CustomEntityNotFoundException("NOT_FOUND", "Text channel not found")
        );
        if (!textChannel.getOwner().getId().equals(owner.getId())) {
            throw new CustomIllegalArgumentException("NO_AUTHORITY", "You are not the owner");
        }
        textChannelMongoRepository.delete(textChannel);
    }
}
