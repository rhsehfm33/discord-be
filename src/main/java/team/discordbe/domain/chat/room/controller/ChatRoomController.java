package team.discordbe.domain.chat.room.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import team.discordbe.domain.chat.room.dto.ChatRoomRequestDto;
import team.discordbe.domain.chat.room.dto.ChatRoomResponseDto;
import team.discordbe.domain.chat.room.service.ChatRoomService;
import team.discordbe.global.exception.CustomEntityNotFoundException;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@RequestMapping("/chat-rooms")
public class ChatRoomController {
    private final ChatRoomService chatRoomService;

    @PostMapping
    public ChatRoomResponseDto create(
        Authentication authentication, @RequestBody ChatRoomRequestDto chatRoomRequestDto
    ) {
        return chatRoomService.create(authentication, chatRoomRequestDto);
    }

    @GetMapping
    public List<ChatRoomResponseDto> getAll(Authentication authentication) {
        return chatRoomService.getAll(authentication);
    }

    @GetMapping("/{chatRoomId}")
    public ChatRoomResponseDto get(Authentication authentication, @PathVariable String chatRoomId)
        throws CustomEntityNotFoundException {
        return chatRoomService.get(authentication, chatRoomId);
    }

    @PutMapping
    public ChatRoomResponseDto update(
        Authentication authentication, @RequestBody ChatRoomRequestDto chatRoomRequestDto
    ) throws CustomEntityNotFoundException {
        return chatRoomService.update(authentication, chatRoomRequestDto);
    }

    @DeleteMapping("/{chatRoomId}")
    public void delete(Authentication authentication, @PathVariable String chatRoomId)
        throws CustomEntityNotFoundException {
        chatRoomService.delete(authentication, chatRoomId);
    }
}
