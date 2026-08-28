package discord.chat.api.interfaces.chat.room;

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

import discord.chat.api.domain.chat.room.ChatRoomService;
import discord.chat.common.exception.CustomEntityNotFoundException;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@RequestMapping("/chat-rooms")
public class ChatRoomController {
    private final ChatRoomService chatRoomService;

    @PostMapping
    public ChatRoomResponse create(
        Authentication authentication, @RequestBody ChatRoomRequest chatRoomRequest
    ) {
        return chatRoomService.create(authentication, chatRoomRequest);
    }

    @GetMapping
    public List<ChatRoomResponse> getAll(Authentication authentication) {
        return chatRoomService.getAll(authentication);
    }

    @GetMapping("/{chatRoomId}")
    public ChatRoomResponse get(Authentication authentication, @PathVariable String chatRoomId)
        throws CustomEntityNotFoundException {
        return chatRoomService.get(authentication, chatRoomId);
    }

    @PutMapping
    public ChatRoomResponse update(
        Authentication authentication, @RequestBody ChatRoomRequest chatRoomRequest
    ) throws CustomEntityNotFoundException {
        return chatRoomService.update(authentication, chatRoomRequest);
    }

    @DeleteMapping("/{chatRoomId}")
    public void delete(Authentication authentication, @PathVariable String chatRoomId)
        throws CustomEntityNotFoundException {
        chatRoomService.delete(authentication, chatRoomId);
    }
}
