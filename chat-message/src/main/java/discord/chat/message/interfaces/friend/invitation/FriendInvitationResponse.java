package discord.chat.message.interfaces.friend.invitation;

import discord.chat.common.infrastructure.user.User;
import discord.chat.message.interfaces.friend.SimpleUserResponseDto;
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

