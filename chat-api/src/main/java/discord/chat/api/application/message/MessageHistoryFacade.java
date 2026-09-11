package discord.chat.api.application.message;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import discord.chat.common.exception.CustomIllegalArgumentException;
import discord.chat.api.domain.message.MessageHistoryService;
import discord.chat.api.infrastructure.client.userapi.UserApiClient;
import discord.chat.api.infrastructure.client.userapi.InternalUserProfileResponse;
import discord.chat.api.interfaces.message.MessageResponse;
import discord.chat.api.interfaces.message.MessageSenderResponse;
import discord.chat.api.interfaces.message.ChatMessageResponse;
import discord.chat.api.domain.chat.channel.ChannelAccessService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageHistoryFacade {
    private final UserApiClient userApiClient;
    private final ChannelAccessService channelAccessService;
    private final MessageHistoryService messageHistoryService;

    public List<ChatMessageResponse> getMessages(
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
        Map<String, InternalUserProfileResponse> profilesById = userApiClient
            .getUserProfiles(senderIds)
            .stream()
            .collect(Collectors.toMap(InternalUserProfileResponse::id, Function.identity()));

        return messages.stream()
            .map(message -> toResponse(message, profilesById.get(message.getSenderId())))
            .toList();
    }

    private void verifyChannelAccess(String userId, String chatRoomId, String textChannelId) {
        boolean hasAccess = channelAccessService.hasTextChannelAccess(userId, chatRoomId, textChannelId);
        if (!hasAccess) {
            throw new AccessDeniedException("No access to text channel");
        }
    }

    private ChatMessageResponse toResponse(
        MessageResponse message,
        InternalUserProfileResponse profile
    ) {
        MessageSenderResponse sender = profile == null
            ? new MessageSenderResponse(message.getSenderId(), "Unknown", null)
            : new MessageSenderResponse(profile.id(), profile.nickName(), profile.imageUrl());

        return new ChatMessageResponse(
            message.getMessageId(),
            message.getChatRoomId(),
            message.getTextChannelId(),
            sender,
            message.getContent(),
            message.getCreatedAt()
        );
    }
}
