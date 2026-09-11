package discord.chat.api.domain.chat.channel;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.access.AccessDeniedException;
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
public class ChannelAccessService {
    private final UserMongoRepository userMongoRepository;
    private final ChatSubscriptMongoRepository chatSubscriptMongoRepository;
    private final TextChannelMongoRepository textChannelMongoRepository;

    public List<AccessibleTextChannelResponse> getAccessibleTextChannels(String userId) {
        User user = userMongoRepository.findById(userId).orElseThrow(
            () -> new AccessDeniedException("User not found")
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
