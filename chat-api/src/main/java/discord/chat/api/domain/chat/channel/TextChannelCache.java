package discord.chat.api.domain.chat.channel;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import discord.chat.common.infrastructure.chat.channel.TextChannel;
import discord.chat.common.infrastructure.chat.channel.TextChannelMongoRepository;
import discord.chat.common.infrastructure.chat.room.ChatRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class TextChannelCache {
    private final TextChannelMongoRepository textChannelMongoRepository;
    private final Cache<String, String> chatRoomIds = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(Duration.ofMinutes(10))
        .build();

    public boolean belongsToRoom(String textChannelId, String chatRoomId) {
        if (textChannelId == null || chatRoomId == null) {
            return false;
        }
        String cachedRoomId = chatRoomIds.get(textChannelId, id ->
            textChannelMongoRepository.findById(id)
                .map(TextChannel::getChatRoom)
                .map(ChatRoom::getId)
                .orElse(null)
        );
        return chatRoomId.equals(cachedRoomId);
    }

    public void invalidate(String textChannelId) {
        chatRoomIds.asMap().compute(textChannelId, (id, chatRoomId) -> null);
    }
}
