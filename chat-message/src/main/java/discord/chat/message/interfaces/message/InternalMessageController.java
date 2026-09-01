package discord.chat.message.interfaces.message;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import discord.chat.message.domain.message.MessageCreationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/internal/messages")
@RequiredArgsConstructor
public class InternalMessageController {
    private final MessageCreationService messageCreationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_internal')")
    public MessageResponse create(@Valid @RequestBody CreateMessageRequest request) {
        return messageCreationService.create(request);
    }
}
