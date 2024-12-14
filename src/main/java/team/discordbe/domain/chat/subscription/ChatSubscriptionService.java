package team.discordbe.domain.chat.subscription;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import team.discordbe.domain.chat.room.ChatRoomType;
import team.discordbe.infrastructure.chat.room.ChatRoom;
import team.discordbe.infrastructure.chat.room.ChatRoomMongoRepository;
import team.discordbe.infrastructure.chat.subsription.ChatSubscriptMongoRepository;
import team.discordbe.infrastructure.chat.subsription.ChatSubscription;
import team.discordbe.infrastructure.user.User;
import team.discordbe.interfaces.chat.room.ChatRoomResponse;
import team.discordbe.interfaces.common.exception.CustomEntityNotFoundException;
import team.discordbe.interfaces.common.exception.CustomResourceConflictException;

@Service
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ChatSubscriptionService {
    private final ChatSubscriptMongoRepository chatSubscriptMongoRepository;
    private final ChatRoomMongoRepository chatRoomMongoRepository;

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

        return new ChatRoomResponse(chatRoom, chatRoom.getOwner().equals(user));
    }

    public void unsubscribe(Authentication authentication, String chatRoomId)
        throws CustomEntityNotFoundException {
        User user = (User) authentication.getPrincipal();
        ChatRoom chatRoom = chatRoomMongoRepository.findById(chatRoomId).orElseThrow(
            () -> new CustomEntityNotFoundException("NOT_FOUND", "Chat room not found")
        );
        ChatSubscription chatSubscription = chatSubscriptMongoRepository.findByUserAndChatRoom(user, chatRoom).orElseThrow(
            () -> new CustomEntityNotFoundException("NOT_FOUND", "Chat subscription not found")
        );
        chatSubscriptMongoRepository.delete(chatSubscription);
    }
}
