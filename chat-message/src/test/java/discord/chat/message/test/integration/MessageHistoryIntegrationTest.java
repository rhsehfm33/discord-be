package discord.chat.message.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import discord.chat.common.exception.CustomIllegalArgumentException;
import discord.chat.message.domain.message.MessageHistoryService;
import discord.chat.message.infrastructure.message.ChatMessage;
import discord.chat.message.infrastructure.message.ChatMessageMongoRepository;
import discord.chat.message.interfaces.message.MessageResponse;
import discord.chat.message.test.BaseIntegrationTest;

class MessageHistoryIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private MessageHistoryService messageHistoryService;

    @Autowired
    private ChatMessageMongoRepository chatMessageMongoRepository;

    @Test
    void getMessagesPaginatesNewestFirstWithoutCrossingChannels() throws Exception {
        List<ChatMessage> messages = new ArrayList<>();
        for (int index = 0; index < 55; index++) {
            messages.add(new ChatMessage("sender", "room", "channel", "message-" + index));
        }
        chatMessageMongoRepository.saveAll(messages);
        chatMessageMongoRepository.save(
            new ChatMessage("sender", "room", "other-channel", "not-included")
        );

        List<MessageResponse> firstPage = messageHistoryService.getMessages(
            "room", "channel", null, 50
        );
        List<MessageResponse> secondPage = messageHistoryService.getMessages(
            "room", "channel", firstPage.get(firstPage.size() - 1).getMessageId(), 50
        );

        assertThat(firstPage).hasSize(50);
        assertThat(secondPage).hasSize(5);
        assertThat(firstPage.get(0).getContent()).isEqualTo("message-54");
        assertThat(secondPage.get(secondPage.size() - 1).getContent()).isEqualTo("message-0");
        assertThat(firstPage).extracting(MessageResponse::getMessageId)
            .doesNotContainAnyElementsOf(
                secondPage.stream().map(MessageResponse::getMessageId).toList()
            );
    }

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
