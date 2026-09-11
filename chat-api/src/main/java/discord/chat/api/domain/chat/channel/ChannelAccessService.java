package discord.chat.api.domain.chat.channel;

import discord.chat.common.infrastructure.chat.channel.TextChannel;
import discord.chat.common.infrastructure.chat.channel.TextChannelMongoRepository;
import discord.chat.common.infrastructure.chat.room.ChatRoom;
import discord.chat.common.infrastructure.chat.subsription.ChatSubscriptMongoRepository;
import discord.chat.common.infrastructure.chat.subsription.ChatSubscription;
import discord.chat.common.infrastructure.user.User;
import discord.chat.common.infrastructure.user.UserMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChannelAccessService {
    private final UserMongoRepository userMongoRepository;
    private final ChatSubscriptMongoRepository chatSubscriptRepository;
    private final TextChannelMongoRepository textChannelRepository;

    public boolean hasTextChannelAccess(String userId, String chatRoomId, String textChannelId) {
        boolean isUserSubscribed = chatSubscriptRepository.existsByUserIdAndChatRoomId(userId, chatRoomId);
        if (!isUserSubscribed) {
            return false;
        }
        boolean isTextChannelExist = textChannelRepository.existsByIdAndChatRoomId(
                textChannelId,
                chatRoomId
        );
        return isTextChannelExist;
    }

    public List<TextChannel> getAccessibleTextChannels(String userId) {
        User user = userMongoRepository.findById(userId).orElseThrow(
            () -> new AccessDeniedException("User not found")
        );

        List<ChatRoom> subscribedChatRooms = chatSubscriptRepository.findAllByUser(user)
            .stream()
            .map(ChatSubscription::getChatRoom)
            .toList();

        return textChannelRepository.findAllByChatRoomIn(subscribedChatRooms);
    }
}
