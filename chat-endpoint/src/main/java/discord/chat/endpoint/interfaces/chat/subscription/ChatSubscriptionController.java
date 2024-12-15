package discord.chat.endpoint.interfaces.chat.subscription;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import discord.chat.endpoint.domain.chat.subscription.ChatSubscriptionService;
import discord.chat.endpoint.interfaces.chat.room.ChatRoomResponse;
import discord.chat.endpoint.interfaces.common.exception.CustomEntityNotFoundException;
import discord.chat.endpoint.interfaces.common.exception.CustomResourceConflictException;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ChatSubscriptionController {
    private final ChatSubscriptionService chatSubscriptionService;

    @PostMapping("/chat-rooms/{chatRoomId}/subscriptions")
    public ChatRoomResponse subscribe(
        Authentication authentication, @PathVariable String chatRoomId
    ) throws CustomEntityNotFoundException, CustomResourceConflictException {
        return chatSubscriptionService.subscribe(authentication, chatRoomId);
    }

    @DeleteMapping("/chat-rooms/{chatRoomId}/subscriptions")
    public void unsubscribe(
        Authentication authentication, @PathVariable String chatRoomId
    ) throws CustomEntityNotFoundException {
        chatSubscriptionService.unsubscribe(authentication, chatRoomId);
    }
}
