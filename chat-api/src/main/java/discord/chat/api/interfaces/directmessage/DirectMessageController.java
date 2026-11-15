package discord.chat.api.interfaces.directmessage;

import java.util.List;

import discord.chat.api.application.message.ChatMessageFacade;
import discord.chat.api.application.message.ChatMessageService;
import discord.chat.api.domain.directmessage.DirectMessageService;
import discord.chat.common.exception.CustomEntityNotFoundException;
import discord.chat.common.exception.CustomIllegalArgumentException;
import discord.chat.common.infrastructure.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/direct-messages")
public class DirectMessageController {
    private final DirectMessageService directMessageService;
    private final ChatMessageService chatMessageService;
    private final ChatMessageFacade chatMessageFacade;

    @PostMapping
    public DirectMessageResponse open(
        Authentication authentication, @Valid @RequestBody OpenDirectMessageRequest request
    ) throws CustomEntityNotFoundException {
        return directMessageService.open(authentication, request.friendshipId());
    }

    @GetMapping("/{directMessageId}")
    public DirectMessageResponse get(Authentication authentication, @PathVariable String directMessageId)
        throws CustomEntityNotFoundException {
        return directMessageService.get(authentication, directMessageId);
    }

    @PostMapping("/{directMessageId}/messages")
    public DirectMessageMessageResponse saveMessage(
        Authentication authentication,
        @PathVariable String directMessageId,
        @Valid @RequestBody DirectMessageMessageRequest request
    ) throws CustomEntityNotFoundException {
        return chatMessageService.saveDirectMessage(authentication, directMessageId, request.content());
    }

    @GetMapping("/{directMessageId}/messages")
    @PreAuthorize("isAuthenticated()")
    public List<DirectMessageMessageResponse> getMessages(
        Authentication authentication,
        @PathVariable String directMessageId,
        @RequestParam(name = "before", required = false) String beforeMessageId
    ) throws CustomEntityNotFoundException, CustomIllegalArgumentException {
        User user = (User) authentication.getPrincipal();
        return chatMessageFacade.getDirectMessageHistory(user.getId(), directMessageId, beforeMessageId);
    }
}
