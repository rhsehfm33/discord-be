package team.discordbe.domain.invitation.friend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import team.discordbe.domain.invitation.friend.dto.FriendInvitationRequestDto;
import team.discordbe.domain.invitation.friend.dto.FriendInvitationResponseDto;
import team.discordbe.domain.invitation.friend.service.FriendInvitationService;
import team.discordbe.global.exception.CustomEntityNotFoundException;

@RestController
@RequestMapping("/friend-invitations")
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
public class FriendInvitationController {
     private final FriendInvitationService friendInvitationService;

     @PostMapping
     void invite(Authentication authentication, FriendInvitationRequestDto friendInvitationRequestDto)
         throws CustomEntityNotFoundException {
         friendInvitationService.invite(authentication, friendInvitationRequestDto.getNickName());
     }

    @GetMapping
    public List<FriendInvitationResponseDto> getFriendInvitations(
        Authentication authentication, @RequestParam boolean isPassive
    ) {
         return friendInvitationService.getFriendInvitations(authentication, isPassive);
    }

    @PutMapping("/{invitationId}")
    public void accept(Authentication authentication, @PathVariable String invitationId) throws
        CustomEntityNotFoundException {
         friendInvitationService.accept(authentication, invitationId);
    }

    @DeleteMapping("/{invitationId}")
    public void cancel(Authentication authentication, @PathVariable String invitationId)
        throws CustomEntityNotFoundException {
        friendInvitationService.cancel(authentication, invitationId);
    }
}
