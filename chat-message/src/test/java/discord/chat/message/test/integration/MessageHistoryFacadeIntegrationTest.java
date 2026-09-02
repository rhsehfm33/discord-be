package discord.chat.message.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;

import discord.chat.message.application.message.MessageHistoryFacade;
import discord.chat.message.infrastructure.client.chatapi.AccessibleTextChannelResponse;
import discord.chat.message.infrastructure.client.chatapi.ChatApiClient;
import discord.chat.message.infrastructure.client.chatapi.InternalUserProfileResponse;
import discord.chat.message.infrastructure.message.ChatMessage;
import discord.chat.message.infrastructure.message.ChatMessageMongoRepository;
import discord.chat.message.interfaces.message.ReceivedTextMessageResponse;
import discord.chat.message.test.BaseIntegrationTest;

class MessageHistoryFacadeIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private MessageHistoryFacade messageHistoryFacade;

    @Autowired
    private ChatMessageMongoRepository chatMessageMongoRepository;

    @MockBean
    private ChatApiClient chatApiClient;

    @Test
    void getMessagesAuthorizesAndAddsSenderProfiles() throws Exception {
        chatMessageMongoRepository.saveAll(List.of(
            new ChatMessage("sender", "room", "channel", "hello"),
            new ChatMessage("missing", "room", "channel", "world")
        ));
        when(chatApiClient.getAccessibleTextChannels("user"))
            .thenReturn(List.of(new AccessibleTextChannelResponse("room", "channel")));
        when(chatApiClient.getUserProfiles(Set.of("sender", "missing")))
            .thenReturn(List.of(new InternalUserProfileResponse("sender", "Sender", "image.png")));

        List<ReceivedTextMessageResponse> messages = messageHistoryFacade.getMessages(
            "user", "room", "channel", null, 50
        );

        assertThat(messages).hasSize(2);
        assertThat(messages)
            .filteredOn(message -> message.getSender().getId().equals("sender"))
            .singleElement()
            .satisfies(message -> assertThat(message.getSender().getNickName()).isEqualTo("Sender"));
        assertThat(messages)
            .filteredOn(message -> message.getSender().getId().equals("missing"))
            .singleElement()
            .satisfies(message -> assertThat(message.getSender().getNickName()).isEqualTo("Unknown"));
    }

    @Test
    void getMessagesRejectsUnauthorizedChannels() {
        when(chatApiClient.getAccessibleTextChannels("user")).thenReturn(List.of());

        assertThatThrownBy(() -> messageHistoryFacade.getMessages(
            "user", "room", "channel", null, 50
        )).isInstanceOf(AccessDeniedException.class);
    }
}
