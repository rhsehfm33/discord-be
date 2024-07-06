package team.discordbe.domain.friend.invitation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FriendInvitationRequestDto {
    private final String id;
    private final String nickName;
}
