package team.discordbe.interfaces.friend.invitation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import team.discordbe.infrastructure.user.User;
import team.discordbe.interfaces.user.SimpleUserResponseDto;

@Getter
@AllArgsConstructor
public class FriendInvitationResponse {
    private final String invitationId;
    private final SimpleUserResponseDto user;

    public FriendInvitationResponse(String invitationId, User user) {
        this.invitationId = invitationId;
        this.user = new SimpleUserResponseDto(user);
    }
}
