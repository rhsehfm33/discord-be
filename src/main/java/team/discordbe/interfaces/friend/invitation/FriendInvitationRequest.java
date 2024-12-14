package team.discordbe.interfaces.friend.invitation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FriendInvitationRequest {
    private final String id;
    private final String nickName;
}
