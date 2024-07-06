package team.discordbe.domain.chat.room.service;

import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import team.discordbe.domain.chat.room.dto.ChatRoomRequestDto;
import team.discordbe.domain.chat.room.dto.ChatRoomResponseDto;
import team.discordbe.domain.chat.room.model.ChatRoom;
import team.discordbe.domain.chat.room.repository.ChatRoomRepository;
import team.discordbe.domain.user.model.User;
import team.discordbe.global.exception.CustomEntityNotFoundException;

@Service
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;

    public ChatRoomResponseDto create(Authentication authentication, ChatRoomRequestDto chatRoomRequestDto) {
        User owner = (User) authentication.getPrincipal();
        ChatRoom newChatRoom = new ChatRoom(owner, chatRoomRequestDto);
        return new ChatRoomResponseDto(chatRoomRepository.save(newChatRoom));
    }

    public void delete(Authentication authentication, String chatRoomId) throws CustomEntityNotFoundException {
        User owner = (User) authentication.getPrincipal();
        Optional<ChatRoom> chatRoomOptional = chatRoomRepository.findByIdAndOwner(chatRoomId, owner);
        if (chatRoomOptional.isPresent()) {
            chatRoomRepository.deleteById(chatRoomId);
        } else {
            throw new CustomEntityNotFoundException("NOT_FOUND", "Chat room is not found under given conditions");
        }
    }
}
