package discord.chat.api.interfaces.directmessage;

import discord.chat.api.domain.directmessage.DirectMessageService;
import discord.chat.common.exception.CustomEntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class DirectMessageSocketController {
    private final DirectMessageService directMessageService;

    @MessageMapping("/sendDirectMessage")
    @SendToUser(value = "/channel/errors", broadcast = false)
    public DirectMessageSendError handleDirectMessage(
        @Valid DirectMessageMessageRequest request,
        Authentication authentication
    ) throws CustomEntityNotFoundException {
        boolean saved = directMessageService.sendDirectMessage(
            authentication, request.directMessageId(), request.content()
        );
        if (!saved) {
            return new DirectMessageSendError("DIRECT_MESSAGE_SAVE_FAILED", request.directMessageId());
        }
        return null;
    }

    public record DirectMessageSendError(String code, String directMessageId) {
    }
}
