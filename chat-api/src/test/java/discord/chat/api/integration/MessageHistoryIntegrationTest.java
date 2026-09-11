package discord.chat.api.integration;

import discord.chat.common.exception.CustomIllegalArgumentException;
import discord.chat.api.domain.message.MessageHistoryService;
import discord.chat.api.infrastructure.message.ChatMessage;
import discord.chat.api.infrastructure.message.ChatMessageRepository;
import discord.chat.api.infrastructure.message.ChatMessage;
import discord.chat.api.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageHistoryIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private MessageHistoryService messageHistoryService;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    // Checks that history pages stay in one channel and do not repeat messages.
    @Test
    void getMessagesPaginatesNewestFirstWithoutCrossingChannels() throws Exception {
        List<ChatMessage> channelMessages = new ArrayList<>();
        for (int messageIndex = 0; messageIndex < 55; messageIndex++) {
            channelMessages.add(new ChatMessage("sender", "room", "channel", "message-" + messageIndex));
        }
        chatMessageRepository.saveAll(channelMessages);
        chatMessageRepository.save(
            new ChatMessage("sender", "room", "other-channel", "not-included")
        );

        List<ChatMessage> firstPageMessages = messageHistoryService.getMessages(
            "room", "channel", null, 50
        );
        List<ChatMessage> secondPageMessages = messageHistoryService.getMessages(
            "room", "channel", firstPageMessages.get(firstPageMessages.size() - 1).getId(), 50
        );

        assertThat(firstPageMessages).hasSize(50);
        assertThat(secondPageMessages).hasSize(5);
        assertThat(firstPageMessages.get(0).getContent()).isEqualTo("message-54");
        assertThat(secondPageMessages.get(secondPageMessages.size() - 1).getContent()).isEqualTo("message-0");
        assertThat(firstPageMessages).extracting(ChatMessage::getId)
            .doesNotContainAnyElementsOf(
                secondPageMessages.stream().map(ChatMessage::getId).toList()
            );
    }

    // Checks that invalid message cursors and page sizes are rejected.
    @Test
    void getMessagesRejectsInvalidCursorAndLimit() {
        assertThatThrownBy(() -> messageHistoryService.getMessages("room", "channel", "bad", 50))
            .isInstanceOf(CustomIllegalArgumentException.class);
        assertThatThrownBy(() -> messageHistoryService.getMessages("room", "channel", null, 0))
            .isInstanceOf(CustomIllegalArgumentException.class);
        assertThatThrownBy(() -> messageHistoryService.getMessages("room", "channel", null, 101))
            .isInstanceOf(CustomIllegalArgumentException.class);
    }
}
