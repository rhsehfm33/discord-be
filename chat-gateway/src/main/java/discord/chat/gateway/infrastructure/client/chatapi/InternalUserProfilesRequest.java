package discord.chat.gateway.infrastructure.client.chatapi;

import java.util.List;

public record InternalUserProfilesRequest(List<String> userIds) {
}
