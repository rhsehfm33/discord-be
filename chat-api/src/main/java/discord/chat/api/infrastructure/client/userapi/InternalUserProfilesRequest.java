package discord.chat.api.infrastructure.client.userapi;

import java.util.List;

public record InternalUserProfilesRequest(List<String> userIds) {
}
