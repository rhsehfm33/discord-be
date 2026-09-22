package discord.chat.api.domain.chat.subscription;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import discord.chat.api.application.session.ChatSessionEvent;
import discord.chat.api.infrastructure.redis.ChatSessionEventPublisher;
import discord.chat.common.infrastructure.chat.room.ChatRoomType;
import discord.chat.common.infrastructure.chat.room.ChatRoom;
import discord.chat.common.infrastructure.chat.room.ChatRoomMongoRepository;
import discord.chat.common.infrastructure.chat.subsription.ChatSubscriptMongoRepository;
import discord.chat.common.infrastructure.chat.subsription.ChatSubscription;
import discord.chat.common.infrastructure.user.User;
import discord.chat.api.interfaces.chat.room.ChatRoomResponse;
import discord.chat.common.exception.CustomEntityNotFoundException;
import discord.chat.common.exception.CustomResourceConflictException;

@Service
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Transactional
public class ChatSubscriptionService {
    private final ChatSubscriptMongoRepository chatSubscriptMongoRepository;
    private final ChatRoomMongoRepository chatRoomMongoRepository;
    private final ChatSessionEventPublisher chatSessionEventPublisher;

    public ChatRoomResponse subscribe(Authentication authentication, String chatRoomId)
        throws CustomEntityNotFoundException, CustomResourceConflictException {
        User user = (User) authentication.getPrincipal();
        ChatRoom chatRoom = chatRoomMongoRepository.findByIdAndType(chatRoomId, ChatRoomType.COMMUNITY).orElseThrow(
            () -> new CustomEntityNotFoundException("NOT_FOUND", "Chat room not found")
        );
        if (chatSubscriptMongoRepository.findByUserAndChatRoom(user, chatRoom).isPresent()) {
            throw new CustomResourceConflictException("CONFLICT", "Chat subscription already exists");
        }
        ChatSubscription chatSubscription = new ChatSubscription(user, chatRoom);
        chatSubscriptMongoRepository.save(chatSubscription);
        chatSessionEventPublisher.publishAfterCommit(
            ChatSessionEvent.join(user.getId(), user.getNickName(), chatRoomId)
        );

        return new ChatRoomResponse(chatRoom, chatRoom.getOwner().equals(user));
    }

    public void unsubscribe(Authentication authentication, String chatRoomId)
        throws CustomEntityNotFoundException {
        User user = (User) authentication.getPrincipal();
        ChatRoom chatRoom = chatRoomMongoRepository.findById(chatRoomId).orElseThrow(
            () -> new CustomEntityNotFoundException("NOT_FOUND", "Chat room not found")
        );
        ChatSubscription chatSubscription = chatSubscriptMongoRepository.findByUserAndChatRoom(user, chatRoom)
            .orElseThrow(() -> new CustomEntityNotFoundException("NOT_FOUND", "Chat subscription not found"));
        chatSubscriptMongoRepository.delete(chatSubscription);
        chatSessionEventPublisher.publishAfterCommit(
            ChatSessionEvent.leave(user.getId(), user.getNickName(), chatRoomId)
        );
    }
}

