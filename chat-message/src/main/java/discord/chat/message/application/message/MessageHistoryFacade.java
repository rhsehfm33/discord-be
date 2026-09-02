package discord.chat.message.application.message;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import discord.chat.common.exception.CustomIllegalArgumentException;
import discord.chat.message.domain.message.MessageHistoryService;
import discord.chat.message.infrastructure.client.chatapi.AccessibleTextChannelResponse;
import discord.chat.message.infrastructure.client.chatapi.ChatApiClient;
import discord.chat.message.infrastructure.client.chatapi.InternalUserProfileResponse;
import discord.chat.message.interfaces.message.MessageResponse;
import discord.chat.message.interfaces.message.MessageSenderResponse;
import discord.chat.message.interfaces.message.ReceivedTextMessageResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageHistoryFacade {
    private final ChatApiClient chatApiClient;
    private final MessageHistoryService messageHistoryService;

    public List<ReceivedTextMessageResponse> getMessages(
        String userId,
        String chatRoomId,
        String textChannelId,
        String beforeMessageId,
        int limit
    ) throws CustomIllegalArgumentException {
        verifyChannelAccess(userId, chatRoomId, textChannelId);

        List<MessageResponse> messages = messageHistoryService.getMessages(
            chatRoomId,
            textChannelId,
            beforeMessageId,
            limit
        );
        Set<String> senderIds = messages.stream()
            .map(MessageResponse::getSenderId)
            .collect(Collectors.toSet());
        Map<String, InternalUserProfileResponse> profilesById = chatApiClient
            .getUserProfiles(senderIds)
            .stream()
            .collect(Collectors.toMap(InternalUserProfileResponse::id, Function.identity()));

        return messages.stream()
            .map(message -> toResponse(message, profilesById.get(message.getSenderId())))
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
        MessageResponse message,
        InternalUserProfileResponse profile
    ) {
        MessageSenderResponse sender = profile == null
            ? new MessageSenderResponse(message.getSenderId(), "Unknown", null)
            : new MessageSenderResponse(profile.id(), profile.nickName(), profile.imageUrl());

        return new ReceivedTextMessageResponse(
            message.getMessageId(),
            message.getChatRoomId(),
            message.getTextChannelId(),
            sender,
            message.getContent(),
            message.getCreatedAt()
        );
    }
}
