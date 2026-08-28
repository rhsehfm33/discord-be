package discord.chat.api.interfaces.friend.invitation;

import discord.chat.common.infrastructure.user.User;
import discord.chat.api.interfaces.user.SimpleUserResponseDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

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
