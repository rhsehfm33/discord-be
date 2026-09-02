package discord.chat.gateway.application.message;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import discord.chat.gateway.infrastructure.client.chatapi.AccessibleTextChannelResponse;
import discord.chat.gateway.infrastructure.client.chatapi.ChatApiClient;
import discord.chat.gateway.infrastructure.client.chatapi.InternalUserProfileResponse;
import discord.chat.gateway.infrastructure.client.message.ChatMessageClient;
import discord.chat.gateway.infrastructure.client.message.StoredMessageResponse;
import discord.chat.gateway.interfaces.chat.channel.MessageSenderResponse;
import discord.chat.gateway.interfaces.chat.channel.ReceivedTextMessageResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageHistoryFacade {
    private final ChatApiClient chatApiClient;
    private final ChatMessageClient chatMessageClient;

    public List<ReceivedTextMessageResponse> getMessages(
        String userId,
        String chatRoomId,
        String textChannelId,
        String beforeMessageId,
        int limit
    ) {
        verifyChannelAccess(userId, chatRoomId, textChannelId);

        List<StoredMessageResponse> messages = chatMessageClient.getMessages(
            chatRoomId,
            textChannelId,
            beforeMessageId,
            limit
        );
        Set<String> senderIds = messages.stream()
            .map(StoredMessageResponse::senderId)
            .collect(Collectors.toSet());
        Map<String, InternalUserProfileResponse> profilesById = chatApiClient
            .getUserProfiles(senderIds)
            .stream()
            .collect(Collectors.toMap(InternalUserProfileResponse::id, Function.identity()));

        return messages.stream()
            .map(message -> toResponse(message, profilesById.get(message.senderId())))
            .toList();
    }

    private void verifyChannelAccess(String userId, String chatRoomId, String textChannelId) {
        boolean hasAccess = chatApiClient.getAccessibleTextChannels(userId).stream()
            .anyMatch(channel -> matches(channel, chatRoomId, textChannelId));
        if (!hasAccess) {
            throw new AccessDeniedException("No access to text channel");
        }
    }

    private boolean matches(
        AccessibleTextChannelResponse channel,
        String chatRoomId,
        String textChannelId
    ) {
        return channel.chatRoomId().equals(chatRoomId)
            && channel.textChannelId().equals(textChannelId);
    }

    private ReceivedTextMessageResponse toResponse(
        StoredMessageResponse message,
        InternalUserProfileResponse profile
    ) {
        MessageSenderResponse sender = profile == null
            ? new MessageSenderResponse(message.senderId(), "Unknown", null)
            : new MessageSenderResponse(profile.id(), profile.nickName(), profile.imageUrl());

        return new ReceivedTextMessageResponse(
            message.messageId(),
            message.chatRoomId(),
            message.textChannelId(),
            sender,
            message.content(),
            message.createdAt()
        );
    }
}
