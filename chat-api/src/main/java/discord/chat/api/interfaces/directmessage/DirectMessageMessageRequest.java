package discord.chat.api.interfaces.directmessage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DirectMessageMessageRequest(@NotBlank @Size(max = 2000) String content) {
}
