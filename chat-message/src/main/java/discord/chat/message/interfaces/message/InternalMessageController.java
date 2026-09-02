package discord.chat.message.interfaces.message;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import discord.chat.common.exception.CustomIllegalArgumentException;
import discord.chat.message.domain.message.MessageCreationService;
import discord.chat.message.domain.message.MessageHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalMessageController {
    private final MessageCreationService messageCreationService;
    private final MessageHistoryService messageHistoryService;

    @PostMapping("/chat-rooms/{chatRoomId}/text-channels/{textChannelId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_internal')")
    public MessageResponse create(
        @PathVariable String chatRoomId,
        @PathVariable String textChannelId,
        @Valid @RequestBody CreateMessageRequest request
    ) {
        return messageCreationService.create(chatRoomId, textChannelId, request);
    }

    @GetMapping("/chat-rooms/{chatRoomId}/text-channels/{textChannelId}/messages")
    @PreAuthorize("hasAuthority('SCOPE_internal')")
    public List<MessageResponse> getMessages(
        @PathVariable String chatRoomId,
        @PathVariable String textChannelId,
        @RequestParam(name = "before", required = false) String beforeMessageId,
        @RequestParam(defaultValue = "50") int limit
    ) throws CustomIllegalArgumentException {
        return messageHistoryService.getMessages(chatRoomId, textChannelId, beforeMessageId, limit);
    }
}
