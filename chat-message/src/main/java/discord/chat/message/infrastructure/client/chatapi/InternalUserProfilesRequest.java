package discord.chat.message.infrastructure.client.chatapi;

import java.util.List;

public record InternalUserProfilesRequest(List<String> userIds) {
}
