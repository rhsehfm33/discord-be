package discord.chat.gateway.interfaces.user;

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
import discord.chat.gateway.domain.user.UserService;
import discord.chat.common.exception.CustomAuthorizationError;
import discord.chat.common.exception.CustomEntityNotFoundException;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRequest dto) {
        UserResponse createdUser = userService.createdUser(dto);
        return ResponseEntity.ok().body(createdUser);
    }

    @GetMapping("/users/mine")
    @PreAuthorize("isAuthenticated()")
    public UserResponse getMyUserInfo(Authentication authentication) {
        return userService.getMyUserInfo(authentication);
    }

    @GetMapping("/chat-rooms/{chatRoomId}/users")
    public List<UserResponse> getParticipants(
        Authentication authentication, @PathVariable String chatRoomId
    ) throws CustomAuthorizationError, CustomEntityNotFoundException {
        return userService.getParticipants(authentication, chatRoomId);
    }

    @PatchMapping("/users/{id}")
    public ResponseEntity<UserResponse> updateUser(
        @PathVariable("id") String id, @RequestBody UserRequest dto
    ) throws CustomEntityNotFoundException {
        UserResponse updatedUser = userService.updateUser(id, dto);
        return ResponseEntity.ok().body(updatedUser);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable("id") String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().body("삭제 완료");
    }
}
