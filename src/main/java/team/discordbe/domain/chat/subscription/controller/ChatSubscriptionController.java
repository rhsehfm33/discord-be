package team.discordbe.domain.chat.subscription.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import team.discordbe.domain.chat.room.dto.ChatRoomResponseDto;
import team.discordbe.domain.chat.subscription.service.ChatSubscriptionService;
import team.discordbe.global.exception.CustomEntityNotFoundException;
import team.discordbe.global.exception.CustomResourceConflictException;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ChatSubscriptionController {
    private final ChatSubscriptionService chatSubscriptionService;

    @PostMapping("/chat-rooms/{chatRoomId}/subscriptions")
    public ChatRoomResponseDto subscribe(
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
