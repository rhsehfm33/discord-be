package team.discordbe.domain.chat.channel.text.controller;

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

import lombok.RequiredArgsConstructor;
import team.discordbe.domain.chat.channel.text.dto.TextChannelRequestDto;
import team.discordbe.domain.chat.channel.text.dto.TextChannelResponseDto;
import team.discordbe.domain.chat.channel.text.service.TextChannelService;
import team.discordbe.global.exception.CustomEntityNotFoundException;
import team.discordbe.global.exception.CustomIllegalArgumentException;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TextChannelController {
    private final TextChannelService textChannelService;

    @PostMapping("/text-channels")
    public TextChannelResponseDto create(
        Authentication authentication, @RequestBody TextChannelRequestDto textChannelRequestDto
    ) throws CustomIllegalArgumentException, CustomEntityNotFoundException {
        return textChannelService.create(authentication, textChannelRequestDto);
    }

    @GetMapping("/chat-rooms/{chatRoomId}/text-channels")
    public List<TextChannelResponseDto> getAll(
        Authentication authentication, @PathVariable String chatRoomId
    ) throws CustomEntityNotFoundException {
        return textChannelService.getAllByChatRoom(authentication, chatRoomId);
    }

    @PutMapping("/text-channels")
    public TextChannelResponseDto update(
        Authentication authentication, @RequestBody TextChannelRequestDto textChannelRequestDto
    ) throws CustomIllegalArgumentException, CustomEntityNotFoundException {
        return textChannelService.update(authentication, textChannelRequestDto);
    }

    @DeleteMapping("/text-channels/{textChannelId}")
    public void delete(
        Authentication authentication, @PathVariable("textChannelId") String textChannelId
    ) throws CustomIllegalArgumentException, CustomEntityNotFoundException {
        textChannelService.delete(authentication, textChannelId);
    }
}
