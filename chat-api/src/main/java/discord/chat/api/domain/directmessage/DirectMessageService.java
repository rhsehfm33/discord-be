package discord.chat.api.domain.directmessage;

import discord.chat.api.interfaces.directmessage.DirectMessageResponse;
import discord.chat.api.interfaces.friend.SimpleUserResponseDto;
import discord.chat.common.exception.CustomEntityNotFoundException;
import discord.chat.common.infrastructure.chat.directmessage.DirectMessage;
import discord.chat.common.infrastructure.chat.directmessage.DirectMessageMongoRepository;
import discord.chat.common.infrastructure.friend.friendship.FriendStatus;
import discord.chat.common.infrastructure.friend.friendship.Friendship;
import discord.chat.common.infrastructure.friend.friendship.FriendshipMongoRepository;
import discord.chat.common.infrastructure.user.User;
import discord.chat.common.infrastructure.user.UserMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DirectMessageService {
    private final DirectMessageMongoRepository directMessageMongoRepository;
    private final FriendshipMongoRepository friendshipMongoRepository;
    private final UserMongoRepository userMongoRepository;

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
}
