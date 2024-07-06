package team.discordbe.domain.chat.room.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
@RequestMapping("/chats/rooms")
public class ChatRoomController {
    private final ChatRoomService chatRoomService;

    @PostMapping
    public ChatRoomResponseDto create(Authentication authentication, ChatRoomRequestDto chatRoomRequestDto) {
        return chatRoomService.create(authentication, chatRoomRequestDto);
    }

    @DeleteMapping("/{chatRoomId}")
    public void delete(Authentication authentication, @PathVariable String chatRoomId)
        throws CustomEntityNotFoundException {
        chatRoomService.delete(authentication, chatRoomId);
    }
}
