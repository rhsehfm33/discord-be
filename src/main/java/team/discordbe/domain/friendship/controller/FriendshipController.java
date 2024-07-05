package team.discordbe.domain.friendship.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import team.discordbe.domain.friendship.constant.FriendStatus;
import team.discordbe.domain.friendship.dto.FriendshipRequestDto;
import team.discordbe.domain.friendship.dto.FriendshipResponseDto;
import team.discordbe.domain.friendship.service.FriendshipService;
import team.discordbe.global.exception.CustomEntityNotFoundException;
import team.discordbe.global.exception.CustomIllegalArgumentException;

@RestController
@RequestMapping("/friendships")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@ResponseStatus(HttpStatus.OK)
public class FriendshipController {
    private final FriendshipService friendshipService;

    @GetMapping
    public List<FriendshipResponseDto> getFriendsByStatus(
        Authentication authentication, @RequestParam FriendStatus friendStatus
    ) {
        return friendshipService.getFriendsByStatus(authentication, friendStatus);
    }

    @GetMapping("/status")
    public FriendStatus getFriendshipStatus(
        Authentication authentication, @RequestParam String nickName
    ) throws CustomIllegalArgumentException {
        return friendshipService.getFriendshipStatus(authentication, nickName);
    }

    @PutMapping
    public void updateFriendship(
        Authentication authentication, FriendshipRequestDto friendshipRequestDto
    ) throws CustomIllegalArgumentException, CustomEntityNotFoundException {
        friendshipService.updateFriendship(authentication, friendshipRequestDto);
    }

    @DeleteMapping
    public void deleteFriendship(
        Authentication authentication, @RequestParam String nickName
    ) throws CustomIllegalArgumentException {
        friendshipService.deleteFriendship(authentication, nickName);
    }
}