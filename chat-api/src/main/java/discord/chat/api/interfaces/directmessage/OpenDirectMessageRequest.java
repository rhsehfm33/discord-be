package discord.chat.api.interfaces.directmessage;

import jakarta.validation.constraints.NotBlank;

public record OpenDirectMessageRequest(@NotBlank String friendshipId) {
}
