package discord.chat.message.interfaces.chat.room;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import discord.chat.message.domain.chat.room.ChatRoomParticipantService;
import discord.chat.message.interfaces.chat.room.ChatRoomParticipantResponse;
import discord.chat.common.exception.CustomAuthorizationError;
import discord.chat.common.exception.CustomEntityNotFoundException;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ChatRoomParticipantController {
    private final ChatRoomParticipantService chatRoomParticipantService;

    @GetMapping("/chat-rooms/{chatRoomId}/users")
    public List<ChatRoomParticipantResponse> getParticipants(
        Authentication authentication, @PathVariable String chatRoomId
    ) throws CustomAuthorizationError, CustomEntityNotFoundException {
        return chatRoomParticipantService.getParticipants(authentication, chatRoomId);
    }
}
