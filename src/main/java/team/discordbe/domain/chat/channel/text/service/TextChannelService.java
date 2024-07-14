package team.discordbe.domain.chat.channel.text.service;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.discordbe.domain.chat.channel.text.dto.TextChannelRequestDto;
import team.discordbe.domain.chat.channel.text.dto.TextChannelResponseDto;
import team.discordbe.domain.chat.channel.text.model.TextChannel;
import team.discordbe.domain.chat.channel.text.repository.TextChannelRepository;
import team.discordbe.domain.chat.room.model.ChatRoom;
import team.discordbe.domain.chat.room.repository.ChatRoomRepository;
import team.discordbe.domain.chat.subscription.repository.ChatSubscriptRepository;
import team.discordbe.domain.user.model.User;
import team.discordbe.global.exception.CustomEntityNotFoundException;
import team.discordbe.global.exception.CustomIllegalArgumentException;

@Service
@Transactional
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class TextChannelService {
    private final TextChannelRepository textChannelRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatSubscriptRepository chatSubscriptRepository;

    public TextChannelResponseDto create(
        Authentication authentication, TextChannelRequestDto textChannelRequestDto
    ) throws CustomEntityNotFoundException, CustomIllegalArgumentException {
        User owner = (User) authentication.getPrincipal();
        ChatRoom chatRoom = chatRoomRepository.findById(textChannelRequestDto.getChatRoomId()).orElseThrow(
            () -> new CustomEntityNotFoundException("NOT_FOUND", "Chat room not found")
        );
        if (!chatRoom.getOwner().getId().equals(owner.getId())) {
            throw new CustomIllegalArgumentException("NO_AUTHORITY", "You are not the owner");
        }
        TextChannel textChannel = new TextChannel(textChannelRequestDto.getTitle(), owner, chatRoom);
        return new TextChannelResponseDto(textChannelRepository.save(textChannel));
    }

    public List<TextChannelResponseDto> getAllByChatRoom(
        Authentication authentication, String chatRoomId
    ) throws CustomEntityNotFoundException {
        User owner = (User) authentication.getPrincipal();
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId).orElseThrow(
            () -> new CustomEntityNotFoundException("NOT_FOUND", "Chat room not found")
        );
        chatSubscriptRepository.findByUserAndChatRoom(owner, chatRoom).orElseThrow(
            () -> new CustomEntityNotFoundException("NO_AUTHORITY", "You don't belong to this chat room")
        );

        List<TextChannelResponseDto> textChannelResponseDtos = textChannelRepository.findAllByChatRoom(chatRoom)
            .stream()
            .map(TextChannelResponseDto::new)
            .toList();
        return textChannelResponseDtos;
    }

    public TextChannelResponseDto update(
        Authentication authentication, TextChannelRequestDto textChannelRequestDto
    ) throws CustomEntityNotFoundException, CustomIllegalArgumentException {
        User owner = (User) authentication.getPrincipal();
        TextChannel textChannel = textChannelRepository.findById(textChannelRequestDto.getId()).orElseThrow(
            () -> new CustomEntityNotFoundException("NOT_FOUND", "Text channel not found")
        );
        if (!textChannel.getOwner().getId().equals(owner.getId())) {
            throw new CustomIllegalArgumentException("NO_AUTHORITY", "You are not the owner");
        }
        textChannel.setTitle(textChannelRequestDto.getTitle());
        return new TextChannelResponseDto(textChannelRepository.save(textChannel));
    }

    public void delete(
        Authentication authentication, String textChannelId
    ) throws CustomEntityNotFoundException, CustomIllegalArgumentException {
        User owner = (User) authentication.getPrincipal();
        TextChannel textChannel = textChannelRepository.findById(textChannelId).orElseThrow(
            () -> new CustomEntityNotFoundException("NOT_FOUND", "Text channel not found")
        );
        if (!textChannel.getOwner().getId().equals(owner.getId())) {
            throw new CustomIllegalArgumentException("NO_AUTHORITY", "You are not the owner");
        }
        textChannelRepository.delete(textChannel);
    }
}
