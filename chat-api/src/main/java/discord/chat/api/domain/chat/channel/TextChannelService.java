package discord.chat.api.domain.chat.channel;

import java.util.List;
import java.util.regex.Pattern;

import com.mongodb.MongoException;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import discord.chat.api.application.session.TextChannelEvent;
import discord.chat.api.infrastructure.redis.ChatSessionEventPublisher;
import discord.chat.common.infrastructure.chat.channel.TextChannel;
import discord.chat.common.infrastructure.chat.channel.TextChannelMongoRepository;
import discord.chat.common.infrastructure.chat.room.ChatRoom;
import discord.chat.common.infrastructure.chat.room.ChatRoomMongoRepository;
import discord.chat.common.infrastructure.chat.subsription.ChatSubscriptMongoRepository;
import discord.chat.common.infrastructure.user.User;
import discord.chat.api.interfaces.chat.channel.TextChannelRequest;
import discord.chat.api.interfaces.chat.channel.TextChannelCreationRequest;
import discord.chat.api.interfaces.chat.channel.TextChannelResponse;
import discord.chat.common.exception.CustomEntityNotFoundException;
import discord.chat.common.exception.CustomException;
import discord.chat.common.exception.CustomIllegalArgumentException;
import discord.chat.common.exception.CustomResourceConflictException;

@Service
@Transactional
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class TextChannelService {
    private static final int MAX_TEXT_CHANNELS = 30;
    private static final int MONGO_WRITE_CONFLICT = 112;
    private static final int MAX_TITLE_LENGTH = 100;
    private static final Pattern TITLE_PATTERN = Pattern.compile("[\\p{L}\\p{Nd}_-]+");

    private final TextChannelMongoRepository textChannelMongoRepository;
    private final ChatRoomMongoRepository chatRoomMongoRepository;
    private final ChatSubscriptMongoRepository chatSubscriptMongoRepository;
    private final ChatSessionEventPublisher chatSessionEventPublisher;

    public TextChannelResponse create(
        Authentication authentication, String chatRoomId, TextChannelCreationRequest textChannelCreationRequest
    ) throws CustomEntityNotFoundException, CustomIllegalArgumentException {
        String title = textChannelCreationRequest.getTitle();
        validateTitle(title);
        User owner = (User) authentication.getPrincipal();
        ChatRoom chatRoom = chatRoomMongoRepository.findById(chatRoomId).orElseThrow(
            () -> new CustomEntityNotFoundException("NOT_FOUND", "Chat room not found")
        );
        if (!chatRoom.getOwner().getId().equals(owner.getId())) {
            throw new CustomIllegalArgumentException("NO_AUTHORITY", "You are not the owner");
        }
        if (textChannelMongoRepository.countByChatRoom(chatRoom) >= MAX_TEXT_CHANNELS) {
            throw new CustomIllegalArgumentException(
                "TEXT_CHANNEL_LIMIT_REACHED", "A chat room can have at most 30 text channels."
            );
        }
        TextChannel textChannel = new TextChannel(title, owner, chatRoom);
        TextChannel savedChannel = textChannelMongoRepository.save(textChannel);
        publishChannelEvent(TextChannelEvent.Type.TEXT_CHANNEL_CREATED, savedChannel);
        return new TextChannelResponse(savedChannel);
    }

    private void validateTitle(String title) throws CustomIllegalArgumentException {
        if (title == null || !TITLE_PATTERN.matcher(title).matches()
            || title.codePointCount(0, title.length()) > MAX_TITLE_LENGTH) {
            throw new CustomIllegalArgumentException(
                "INVALID_TEXT_CHANNEL_TITLE",
                "Channel names must be 1-100 characters and contain only letters, numbers, hyphens, or underscores."
            );
        }
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
        validateTitle(textChannelRequest.getTitle());
        User owner = (User) authentication.getPrincipal();
        TextChannel textChannel = textChannelMongoRepository.findById(textChannelRequest.getId()).orElseThrow(
            () -> new CustomEntityNotFoundException("NOT_FOUND", "Text channel not found")
        );
        if (!textChannel.getOwner().getId().equals(owner.getId())) {
            throw new CustomIllegalArgumentException("NO_AUTHORITY", "You are not the owner");
        }
        textChannel.setTitle(textChannelRequest.getTitle());
        TextChannel savedChannel = textChannelMongoRepository.save(textChannel);
        publishChannelEvent(TextChannelEvent.Type.TEXT_CHANNEL_UPDATED, savedChannel);
        return new TextChannelResponse(savedChannel);
    }

    @Transactional(rollbackFor = CustomException.class)
    public void delete(
        Authentication authentication, String textChannelId
    ) throws CustomEntityNotFoundException, CustomIllegalArgumentException, CustomResourceConflictException {
        User owner = (User) authentication.getPrincipal();
        TextChannel textChannel = textChannelMongoRepository.findById(textChannelId).orElseThrow(
            () -> new CustomEntityNotFoundException("NOT_FOUND", "Text channel not found")
        );
        if (!textChannel.getOwner().getId().equals(owner.getId())) {
            throw new CustomIllegalArgumentException("NO_AUTHORITY", "You are not the owner");
        }
        ChatRoom chatRoom = textChannel.getChatRoom();
        try {
            chatRoomMongoRepository.lockForTextChannelDeletion(chatRoom.getId());
        } catch (DataAccessException exception) {
            if (exception.getMostSpecificCause() instanceof MongoException mongoError
                && mongoError.getCode() == MONGO_WRITE_CONFLICT) {
                throw new CustomResourceConflictException("WRITE_CONFLICT", "The chat room changed. Try again.");
            }
            throw exception;
        }
        if (textChannelMongoRepository.countByChatRoom(chatRoom) <= 1) {
            throw new CustomIllegalArgumentException(
                "LAST_TEXT_CHANNEL", "A chat room must have at least one text channel."
            );
        }
        textChannelMongoRepository.delete(textChannel);
        publishChannelEvent(TextChannelEvent.Type.TEXT_CHANNEL_DELETED, textChannel);
    }

    @Transactional(readOnly = true)
    public TextChannelResponse get(Authentication authentication, String textChannelId)
        throws CustomEntityNotFoundException {
        TextChannel channel = textChannelMongoRepository.findById(textChannelId).orElseThrow(
            () -> new CustomEntityNotFoundException("NOT_FOUND", "Text channel not found")
        );
        User user = (User) authentication.getPrincipal();
        if (!chatSubscriptMongoRepository.existsByUserIdAndChatRoomId(user.getId(), channel.getChatRoom().getId())) {
            throw new CustomEntityNotFoundException("NOT_FOUND", "Text channel not found");
        }
        return new TextChannelResponse(channel);
    }

    private void publishChannelEvent(TextChannelEvent.Type type, TextChannel channel) {
        chatSessionEventPublisher.publishToRoomAfterCommit(
            new TextChannelEvent(type, channel.getChatRoom().getId(), channel.getId())
        );
    }
}

