package discord.chat.api.interfaces.user;

import java.util.List;

public record InternalUserProfilesRequest(List<String> userIds) {
}
