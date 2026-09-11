package discord.chat.message.integration;

import discord.chat.message.application.message.MessageHistoryFacade;
import discord.chat.message.interfaces.chat.channel.AccessibleTextChannelResponse;
import discord.chat.message.infrastructure.client.chatapi.ChatApiClient;
import discord.chat.message.infrastructure.client.chatapi.InternalUserProfileResponse;
import discord.chat.message.infrastructure.message.ChatMessage;
import discord.chat.message.infrastructure.message.ChatMessageRepository;
import discord.chat.message.interfaces.message.ChatMessageResponse;
import discord.chat.message.support.BaseIntegrationTest;
import discord.chat.message.domain.chat.channel.ChannelAccessService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class MessageHistoryFacadeIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private MessageHistoryFacade messageHistoryFacade;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @MockBean
    private ChatApiClient chatApiClient;

    @MockBean
    private ChannelAccessService channelAccessService;

    // Checks that message history includes sender profiles after checking access.
    @Test
    void getMessagesAuthorizesAndAddsSenderProfiles() throws Exception {
        chatMessageRepository.saveAll(List.of(
            new ChatMessage("sender", "room", "channel", "hello"),
            new ChatMessage("missing", "room", "channel", "world")
        ));
        when(channelAccessService.getAccessibleTextChannels("user"))
            .thenReturn(List.of(new AccessibleTextChannelResponse("room", "channel")));
        when(chatApiClient.getUserProfiles(Set.of("sender", "missing")))
            .thenReturn(List.of(new InternalUserProfileResponse("sender", "Sender", "image.png")));

        List<ChatMessageResponse> messageResponses = messageHistoryFacade.getMessages(
            "user", "room", "channel", null, 50
        );

        assertThat(messageResponses).hasSize(2);
        assertThat(messageResponses)
            .filteredOn(messageResponse -> messageResponse.getSender().id().equals("sender"))
            .singleElement()
            .satisfies(messageResponse -> assertThat(messageResponse.getSender().nickName()).isEqualTo("Sender"));
        assertThat(messageResponses)
            .filteredOn(messageResponse -> messageResponse.getSender().id().equals("missing"))
            .singleElement()
            .satisfies(messageResponse -> assertThat(messageResponse.getSender().nickName()).isEqualTo("Unknown"));
    }

    // Checks that users cannot read messages from channels they cannot access.
    @Test
    void getMessagesRejectsUnauthorizedChannels() {
        when(channelAccessService.getAccessibleTextChannels("user")).thenReturn(List.of());

        assertThatThrownBy(() -> messageHistoryFacade.getMessages(
            "user", "room", "channel", null, 50
        )).isInstanceOf(AccessDeniedException.class);
    }
}
