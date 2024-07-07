package team.discordbe.domain.chat.subscription.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import team.discordbe.domain.chat.room.constant.ChatRoomType;
import team.discordbe.domain.chat.room.dto.ChatRoomResponseDto;
import team.discordbe.domain.chat.room.model.ChatRoom;
import team.discordbe.domain.chat.room.repository.ChatRoomRepository;
import team.discordbe.domain.chat.subscription.model.ChatSubscription;
import team.discordbe.domain.chat.subscription.repository.ChatSubscriptRepository;
import team.discordbe.domain.user.model.User;
import team.discordbe.global.exception.CustomEntityNotFoundException;

@Service
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ChatSubscriptionService {
    private final ChatSubscriptRepository chatSubscriptRepository;
    private final ChatRoomRepository chatRoomRepository;

    public ChatRoomResponseDto subscribe(Authentication authentication, String chatRoomId)
        throws CustomEntityNotFoundException {
        User user = (User) authentication.getPrincipal();
        ChatRoom chatRoom = chatRoomRepository.findByIdAndType(chatRoomId, ChatRoomType.COMMUNITY).orElseThrow(
            () -> new CustomEntityNotFoundException("NOT_FOUND", "Chat room not found")
        );
        ChatSubscription chatSubscription = new ChatSubscription(user, chatRoom);
        chatSubscriptRepository.save(chatSubscription);

        return new ChatRoomResponseDto(chatRoom);
    }

    public void unsubscribe(Authentication authentication, String chatRoomId)
        throws CustomEntityNotFoundException {
        User user = (User) authentication.getPrincipal();
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId).orElseThrow(
            () -> new CustomEntityNotFoundException("NOT_FOUND", "Chat room not found")
        );
        ChatSubscription chatSubscription = chatSubscriptRepository.findByUserAndChatRoom(user, chatRoom).orElseThrow(
            () -> new CustomEntityNotFoundException("NOT_FOUND", "Chat subscription not found")
        );
        chatSubscriptRepository.delete(chatSubscription);
    }
}
