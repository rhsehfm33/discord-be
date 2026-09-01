package discord.chat.api.interfaces.chat.channel;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import discord.chat.api.domain.chat.channel.InternalChannelAccessService;
import discord.chat.common.exception.CustomEntityNotFoundException;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalChannelAccessController {
    private final InternalChannelAccessService internalChannelAccessService;

    @GetMapping("/{userId}/text-channels")
    @PreAuthorize("hasAuthority('SCOPE_internal')")
    public List<AccessibleTextChannelResponse> getAccessibleTextChannels(@PathVariable String userId)
        throws CustomEntityNotFoundException {
        return internalChannelAccessService.getAccessibleTextChannels(userId);
    }
}
