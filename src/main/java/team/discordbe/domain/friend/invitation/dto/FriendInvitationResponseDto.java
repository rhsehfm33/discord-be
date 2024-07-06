package team.discordbe.domain.friend.invitation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import team.discordbe.domain.user.dto.SimpleUserResponseDto;
import team.discordbe.domain.user.model.User;

@Getter
@AllArgsConstructor
public class FriendInvitationResponseDto {
    private final String invitationId;
    private final SimpleUserResponseDto user;

    public FriendInvitationResponseDto(String invitationId, User user) {
        this.invitationId = invitationId;
        this.user = new SimpleUserResponseDto(user);
    }
}
