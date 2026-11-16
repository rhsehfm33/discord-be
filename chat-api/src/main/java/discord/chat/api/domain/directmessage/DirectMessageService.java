package discord.chat.api.domain.directmessage;

import discord.chat.api.infrastructure.message.ChatMessage;
import discord.chat.api.infrastructure.message.ChatMessageRepository;
import discord.chat.api.infrastructure.redis.DirectMessageRedisBroker;
import discord.chat.api.interfaces.directmessage.DirectMessageMessageResponse;
import discord.chat.api.interfaces.directmessage.DirectMessageResponse;
import discord.chat.api.interfaces.friend.SimpleUserResponseDto;
import discord.chat.api.interfaces.message.MessageSenderResponse;
import discord.chat.common.exception.CustomEntityNotFoundException;
import discord.chat.common.exception.CustomIllegalArgumentException;
import discord.chat.common.infrastructure.chat.directmessage.DirectMessage;
import discord.chat.common.infrastructure.chat.directmessage.DirectMessageMongoRepository;
import discord.chat.common.infrastructure.friend.friendship.FriendStatus;
import discord.chat.common.infrastructure.friend.friendship.Friendship;
import discord.chat.common.infrastructure.friend.friendship.FriendshipMongoRepository;
import discord.chat.common.infrastructure.user.User;
import discord.chat.common.infrastructure.user.UserMongoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DirectMessageService {
    private static final int DIRECT_MESSAGE_PAGE_SIZE = 50;

    private final DirectMessageMongoRepository directMessageMongoRepository;
    private final FriendshipMongoRepository friendshipMongoRepository;
    private final UserMongoRepository userMongoRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final DirectMessageRedisBroker directMessageRedisBroker;

    @PreAuthorize("isAuthenticated()")
    public DirectMessageResponse open(Authentication authentication, String friendshipId)
        throws CustomEntityNotFoundException {
        User user = (User) authentication.getPrincipal();
        Friendship friendship = friendshipMongoRepository.findByIdAndFromUser(friendshipId, user)
            .filter(foundFriendship -> foundFriendship.getFriendStatus() == FriendStatus.FRIEND)
            .orElseThrow(() -> new CustomEntityNotFoundException("NOT_FOUND", "Friendship not found"));
        User friend = friendship.getToUser();
        DirectMessage directMessage = new DirectMessage(user.getId(), friend.getId());
        DirectMessage storedDirectMessage = directMessageMongoRepository.findById(directMessage.getId())
            .orElse(null);
        if (storedDirectMessage == null) {
            try {
                storedDirectMessage = directMessageMongoRepository.insert(directMessage);
            } catch (DuplicateKeyException exception) {
                storedDirectMessage = directMessageMongoRepository.findById(directMessage.getId())
                    .orElseThrow(() -> exception);
            }
        }
        return new DirectMessageResponse(storedDirectMessage.getId(), new SimpleUserResponseDto(friend));
    }

    @PreAuthorize("isAuthenticated()")
    public DirectMessageResponse get(Authentication authentication, String directMessageId)
        throws CustomEntityNotFoundException {
        User user = (User) authentication.getPrincipal();
        DirectMessage directMessage = directMessageMongoRepository.findById(directMessageId)
            .filter(foundDirectMessage -> foundDirectMessage.hasParticipant(user.getId()))
            .orElseThrow(() -> new CustomEntityNotFoundException("NOT_FOUND", "Direct message not found"));
        String friendId = directMessage.getOtherParticipantId(user.getId());
        User friend = userMongoRepository.findById(friendId)
            .orElseThrow(() -> new CustomEntityNotFoundException("NOT_FOUND", "User not found"));
        return new DirectMessageResponse(directMessage.getId(), new SimpleUserResponseDto(friend));
    }

    @PreAuthorize("isAuthenticated()")
    public List<ChatMessage> getDirectMessageHistory(
        String userId, String directMessageId, String beforeMessageId
    ) throws CustomEntityNotFoundException, CustomIllegalArgumentException {
        directMessageMongoRepository.findById(directMessageId)
            .filter(directMessage -> directMessage.hasParticipant(userId))
            .orElseThrow(() -> new CustomEntityNotFoundException("NOT_FOUND", "Direct message not found"));
        if (beforeMessageId != null && !ObjectId.isValid(beforeMessageId)) {
            throw new CustomIllegalArgumentException("INVALID_MESSAGE_CURSOR", "Message cursor must be a valid ObjectId");
        }

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
        DirectMessage directMessage = directMessageMongoRepository.findById(directMessageId)
            .filter(foundDirectMessage -> foundDirectMessage.hasParticipant(sender.getId()))
            .orElseThrow(() -> new CustomEntityNotFoundException("NOT_FOUND", "Direct message not found"));

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
}
