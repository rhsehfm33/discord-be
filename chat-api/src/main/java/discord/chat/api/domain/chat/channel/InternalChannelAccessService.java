package discord.chat.api.domain.chat.channel;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import discord.chat.common.exception.CustomEntityNotFoundException;
import discord.chat.common.infrastructure.chat.channel.TextChannelMongoRepository;
import discord.chat.common.infrastructure.chat.room.ChatRoom;
import discord.chat.common.infrastructure.chat.subsription.ChatSubscription;
import discord.chat.common.infrastructure.chat.subsription.ChatSubscriptMongoRepository;
import discord.chat.common.infrastructure.user.User;
import discord.chat.common.infrastructure.user.UserMongoRepository;
import discord.chat.api.interfaces.chat.channel.AccessibleTextChannelResponse;
import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InternalChannelAccessService {
    private final UserMongoRepository userMongoRepository;
    private final ChatSubscriptMongoRepository chatSubscriptMongoRepository;
    private final TextChannelMongoRepository textChannelMongoRepository;

    public List<AccessibleTextChannelResponse> getAccessibleTextChannels(String userId)
        throws CustomEntityNotFoundException {
        User user = userMongoRepository.findById(userId).orElseThrow(
            () -> new CustomEntityNotFoundException("NOT_FOUND", "User not found")
        );

        List<ChatRoom> subscribedChatRooms = chatSubscriptMongoRepository.findAllByUser(user)
            .stream()
            .map(ChatSubscription::getChatRoom)
            .toList();

        return textChannelMongoRepository.findAllByChatRoomIn(subscribedChatRooms).stream()
            .map(channel -> new AccessibleTextChannelResponse(
                channel.getChatRoom().getId(),
                channel.getId()
            ))
            .toList();
    }
}
