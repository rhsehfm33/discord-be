package discord.chat.api.interfaces.chat.channel;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import discord.chat.api.domain.chat.channel.TextChannelService;
import discord.chat.api.interfaces.common.exception.CustomEntityNotFoundException;
import discord.chat.api.interfaces.common.exception.CustomIllegalArgumentException;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TextChannelController {
    private final TextChannelService textChannelService;

    @PostMapping("/text-channels")
    public TextChannelResponse create(
        Authentication authentication, @RequestBody TextChannelRequest textChannelRequest
    ) throws CustomIllegalArgumentException, CustomEntityNotFoundException {
        return textChannelService.create(authentication, textChannelRequest);
    }

    @GetMapping("/chat-rooms/{chatRoomId}/text-channels")
    public List<TextChannelResponse> getAll(
        Authentication authentication, @PathVariable String chatRoomId
    ) throws CustomEntityNotFoundException {
        return textChannelService.getAllByChatRoom(authentication, chatRoomId);
    }

    @PutMapping("/text-channels")
    public TextChannelResponse update(
        Authentication authentication, @RequestBody TextChannelRequest textChannelRequest
    ) throws CustomIllegalArgumentException, CustomEntityNotFoundException {
        return textChannelService.update(authentication, textChannelRequest);
    }

    @DeleteMapping("/text-channels/{textChannelId}")
    public void delete(
        Authentication authentication, @PathVariable("textChannelId") String textChannelId
    ) throws CustomIllegalArgumentException, CustomEntityNotFoundException {
        textChannelService.delete(authentication, textChannelId);
    }
}
