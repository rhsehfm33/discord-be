package discord.chat.message.interfaces.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateMessageRequest {
    @NotBlank
    private String senderId;

    @NotBlank
    private String chatRoomId;

    @NotBlank
    private String textChannelId;

    @NotBlank
    @Size(max = 2000)
    private String content;
}
