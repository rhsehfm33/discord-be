package team.discordbe.domain.chat.subscription.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import team.discordbe.domain.chat.room.dto.ChatRoomResponseDto;
import team.discordbe.domain.chat.subscription.service.ChatSubscriptionService;
import team.discordbe.global.exception.CustomEntityNotFoundException;
import team.discordbe.global.exception.CustomResourceConflictException;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@RequestMapping("/chats/subscriptions")
public class ChatSubscriptionController {
    private final ChatSubscriptionService chatSubscriptionService;

    @PostMapping
    public ChatRoomResponseDto subscribe(
        Authentication authentication, @RequestParam("chatRoomId") String chatRoomId
    ) throws CustomEntityNotFoundException, CustomResourceConflictException {
        return chatSubscriptionService.subscribe(authentication, chatRoomId);
    }

    @DeleteMapping
    public void unsubscribe(
        Authentication authentication, @RequestParam("chatRoomId") String chatRoomId
    ) throws CustomEntityNotFoundException {
        chatSubscriptionService.unsubscribe(authentication, chatRoomId);
    }
}
