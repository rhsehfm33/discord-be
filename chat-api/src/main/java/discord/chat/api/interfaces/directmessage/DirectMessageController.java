package discord.chat.api.interfaces.directmessage;

import discord.chat.api.domain.directmessage.DirectMessageService;
import discord.chat.common.exception.CustomEntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/direct-messages")
public class DirectMessageController {
    private final DirectMessageService directMessageService;

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
}
