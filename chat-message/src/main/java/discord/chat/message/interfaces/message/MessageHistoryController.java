package discord.chat.message.interfaces.message;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import discord.chat.common.infrastructure.user.User;
import discord.chat.common.exception.CustomIllegalArgumentException;
import discord.chat.message.application.message.MessageHistoryFacade;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat-rooms/{chatRoomId}/text-channels/{textChannelId}/messages")
public class MessageHistoryController {
    private final MessageHistoryFacade messageHistoryFacade;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<ReceivedTextMessageResponse> getMessages(
        Authentication authentication,
        @PathVariable String chatRoomId,
        @PathVariable String textChannelId,
        @RequestParam(name = "before", required = false) String beforeMessageId,
        @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit
    ) throws CustomIllegalArgumentException {
        User user = (User) authentication.getPrincipal();
        return messageHistoryFacade.getMessages(
            user.getId(),
            chatRoomId,
            textChannelId,
            beforeMessageId,
            limit
        );
    }
}
