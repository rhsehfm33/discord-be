package team.discordbe.domain.friendship.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import team.discordbe.domain.friendship.constant.FriendStatus;
import team.discordbe.domain.friendship.service.FriendshipService;
import team.discordbe.domain.user.dto.UserResponseDto;

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FriendshipController {
    private final FriendshipService friendshipService;

    @GetMapping("/status/{status}")
    @ResponseStatus(HttpStatus.OK)
    public List<UserResponseDto> getFriendsByStatus(Authentication authentication, @PathVariable("status") FriendStatus status) {
        return friendshipService.getFriendsByStatus(authentication, status);
    }

    @GetMapping("/{nickName}/status")
    @ResponseStatus(HttpStatus.OK)
    public FriendStatus getFriendshipStatus(Authentication authentication, @PathVariable("nickName") String nickName) {
        return friendshipService.getFriendshipStatus(authentication, nickName);
    }

    @GetMapping("/invitations/received")
    @ResponseStatus(HttpStatus.OK)
    public List<UserResponseDto> getReceivedInvitations(Authentication authentication) {
        return friendshipService.getReceivedInvitations(authentication);
    }

    @PutMapping("/invitations/received/{nickName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acceptReceivedInvitation(Authentication authentication, @PathVariable("nickName") String nickName) {
        friendshipService.acceptFriendInvitation(authentication, nickName);
    }

    @PostMapping("/invitations/sent/{nickName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendFriendInvitation(Authentication authentication, @PathVariable("nickName") String nickName) {
        friendshipService.sendFriendInvitation(authentication, nickName);
    }

    @PostMapping("/blocked/{nickName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void blockUser(Authentication authentication, @PathVariable("nickName") String nickName) {
        friendshipService.blockUser(authentication, nickName);
    }

    @DeleteMapping("/{nickName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFriend(Authentication authentication, @PathVariable("nickName") String nickName) {
        friendshipService.deleteFriend(authentication, nickName);
    }
}