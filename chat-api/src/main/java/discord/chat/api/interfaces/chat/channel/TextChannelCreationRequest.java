package discord.chat.api.interfaces.chat.channel;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TextChannelCreationRequest {
    private String title;
}
