package team.discordbe.domain.user.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import team.discordbe.domain.user.dto.UserRequestDto;
import team.discordbe.domain.user.dto.UserResponseDto;
import team.discordbe.domain.user.services.UserService;
import team.discordbe.global.exception.CustomEntityNotFoundException;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/users")
    public ResponseEntity<UserResponseDto> createUser(@RequestBody UserRequestDto dto) {
        UserResponseDto createdUser = userService.createdUser(dto);
        return ResponseEntity.ok().body(createdUser);
    }

    @GetMapping("/users/mine")
    @PreAuthorize("isAuthenticated()")
    public UserResponseDto getMyUserInfo(Authentication authentication) {
        return userService.getMyUserInfo(authentication);
    }

    @GetMapping("/chat-rooms/{chatRoomId}/users")
    public List<UserResponseDto> getParticipants(
        Authentication authentication, @PathVariable String chatRoomId
    ) {
        return userService.getParticipants(authentication, chatRoomId);
    }

    @PatchMapping("/users/{id}")
    public ResponseEntity<UserResponseDto> updateUser(
        @PathVariable("id") String id, @RequestBody UserRequestDto dto
    ) throws CustomEntityNotFoundException {
        UserResponseDto updatedUser = userService.updateUser(id, dto);
        return ResponseEntity.ok().body(updatedUser);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable("id") String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().body("삭제 완료");
    }
}
