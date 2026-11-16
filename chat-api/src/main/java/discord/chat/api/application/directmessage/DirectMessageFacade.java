package discord.chat.api.application.directmessage;

import discord.chat.api.domain.directmessage.DirectMessageService;
import discord.chat.api.infrastructure.client.userapi.InternalUserProfileResponse;
import discord.chat.api.infrastructure.client.userapi.UserApiClient;
import discord.chat.api.infrastructure.message.ChatMessage;
import discord.chat.api.interfaces.directmessage.DirectMessageMessageResponse;
import discord.chat.api.interfaces.message.MessageSenderResponse;
import discord.chat.common.exception.CustomEntityNotFoundException;
import discord.chat.common.exception.CustomIllegalArgumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DirectMessageFacade {
    private final UserApiClient userApiClient;
    private final DirectMessageService directMessageService;

    public List<DirectMessageMessageResponse> getMessages(
        String userId, String directMessageId, String beforeMessageId
    ) throws CustomEntityNotFoundException, CustomIllegalArgumentException {
        List<ChatMessage> messages = directMessageService.getDirectMessageHistory(
            userId, directMessageId, beforeMessageId
        );
        Map<String, InternalUserProfileResponse> profilesById = getProfilesById(messages);
        return messages.stream()
            .map(message -> toResponse(message, profilesById.get(message.getSenderId())))
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

    private DirectMessageMessageResponse toResponse(ChatMessage message, InternalUserProfileResponse profile) {
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
