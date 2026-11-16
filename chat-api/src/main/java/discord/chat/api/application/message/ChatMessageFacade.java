package discord.chat.api.application.message;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import discord.chat.api.domain.chat.channel.ChannelAccessService;
import discord.chat.api.infrastructure.client.userapi.InternalUserProfileResponse;
import discord.chat.api.infrastructure.client.userapi.UserApiClient;
import discord.chat.api.infrastructure.message.ChatMessage;
import discord.chat.api.interfaces.directmessage.DirectMessageMessageResponse;
import discord.chat.api.interfaces.message.ChatMessageResponse;
import discord.chat.api.interfaces.message.MessageSenderResponse;
import discord.chat.common.exception.CustomEntityNotFoundException;
import discord.chat.common.exception.CustomIllegalArgumentException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatMessageFacade {
    private final UserApiClient userApiClient;
    private final ChannelAccessService channelAccessService;
    private final ChatMessageService chatMessageService;

    public List<ChatMessageResponse> getMessages(
        String userId,
        String chatRoomId,
        String textChannelId,
        String beforeMessageId,
        int limit
    ) throws CustomIllegalArgumentException {
        verifyChannelAccess(userId, chatRoomId, textChannelId);

        List<ChatMessage> messages = chatMessageService.getMessages(
            chatRoomId,
            textChannelId,
            beforeMessageId,
            limit
        );
        Map<String, InternalUserProfileResponse> profilesById = getProfilesById(messages);

        return messages.stream()
            .map(message -> toResponse(message, profilesById.get(message.getSenderId())))
            .toList();
    }

    public List<DirectMessageMessageResponse> getDirectMessageHistory(
        String userId, String directMessageId, String beforeMessageId
    ) throws CustomEntityNotFoundException, CustomIllegalArgumentException {
        List<ChatMessage> messages = chatMessageService.getDirectMessageHistory(userId, directMessageId, beforeMessageId);
        Map<String, InternalUserProfileResponse> profilesById = getProfilesById(messages);
        return messages.stream()
            .map(message -> toDirectMessageResponse(message, profilesById.get(message.getSenderId())))
            .toList();
    }

    private Map<String, InternalUserProfileResponse> getProfilesById(List<ChatMessage> messages) {
        Set<String> senderIds = messages.stream()
            .map(ChatMessage::getSenderId)
            .collect(Collectors.toSet());
        return userApiClient
            .getUserProfiles(senderIds)
            .stream()
            .collect(Collectors.toMap(InternalUserProfileResponse::id, Function.identity()));
    }

    private void verifyChannelAccess(String userId, String chatRoomId, String textChannelId) {
        boolean hasAccess = channelAccessService.hasTextChannelAccess(userId, chatRoomId, textChannelId);
        if (!hasAccess) {
            throw new AccessDeniedException("No access to text channel");
        }
    }

    private ChatMessageResponse toResponse(
        ChatMessage message,
        InternalUserProfileResponse profile
    ) {
        MessageSenderResponse sender = toSenderResponse(message, profile);
        return new ChatMessageResponse(
            message.getId(),
            message.getChatRoomId(),
            message.getTextChannelId(),
            sender,
            message.getContent(),
            message.getCreatedAt()
        );
    }

    private DirectMessageMessageResponse toDirectMessageResponse(
        ChatMessage message, InternalUserProfileResponse profile
    ) {
        MessageSenderResponse sender = toSenderResponse(message, profile);
        return new DirectMessageMessageResponse(
            message.getId(), message.getDirectMessageId(), sender, message.getContent(), message.getCreatedAt()
        );
    }

    private MessageSenderResponse toSenderResponse(ChatMessage message, InternalUserProfileResponse profile) {
        if (profile == null) {
            return new MessageSenderResponse(message.getSenderId(), "Unknown", null);
        }
        return new MessageSenderResponse(profile.id(), profile.nickName(), profile.imageUrl());
    }
}
