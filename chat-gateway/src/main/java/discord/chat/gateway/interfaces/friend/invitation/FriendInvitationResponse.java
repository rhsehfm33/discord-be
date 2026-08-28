package discord.chat.gateway.interfaces.friend.invitation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import discord.chat.common.infrastructure.user.User;
import discord.chat.gateway.interfaces.user.SimpleUserResponseDto;

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
