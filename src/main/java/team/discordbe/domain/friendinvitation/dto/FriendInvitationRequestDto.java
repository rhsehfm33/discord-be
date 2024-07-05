package team.discordbe.domain.friendinvitation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FriendInvitationRequestDto {
    private final String id;
    private final String nickName;
}
