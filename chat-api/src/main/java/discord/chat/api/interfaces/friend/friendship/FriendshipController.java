package discord.chat.api.interfaces.friend.friendship;

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

import discord.chat.common.infrastructure.friend.friendship.FriendStatus;
import discord.chat.api.domain.friend.friendship.FriendshipService;
import discord.chat.common.exception.CustomEntityNotFoundException;
import discord.chat.common.exception.CustomIllegalArgumentException;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/friends/friendships")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@ResponseStatus(HttpStatus.OK)
public class FriendshipController {
    private final FriendshipService friendshipService;

    @GetMapping
    public List<FriendshipResponse> getFriendsByStatus(
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
        Authentication authentication, FriendshipRequest friendshipRequest
    ) throws CustomIllegalArgumentException, CustomEntityNotFoundException {
        friendshipService.updateFriendship(authentication, friendshipRequest);
    }

    @DeleteMapping
    public void deleteFriendship(
        Authentication authentication, @RequestParam String nickName
    ) throws CustomIllegalArgumentException {
        friendshipService.deleteFriendship(authentication, nickName);
    }
}
