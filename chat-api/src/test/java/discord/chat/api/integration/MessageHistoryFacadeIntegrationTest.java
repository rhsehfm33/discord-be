package discord.chat.api.integration;

import discord.chat.api.application.message.MessageHistoryFacade;
import discord.chat.api.infrastructure.client.userapi.UserApiClient;
import discord.chat.api.infrastructure.client.userapi.InternalUserProfileResponse;
import discord.chat.api.infrastructure.message.ChatMessage;
import discord.chat.api.infrastructure.message.ChatMessageRepository;
import discord.chat.api.interfaces.message.ChatMessageResponse;
import discord.chat.api.support.BaseIntegrationTest;
import discord.chat.api.domain.chat.channel.ChannelAccessService;
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
    private UserApiClient userApiClient;

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
            .thenReturn(java.util.Map.of("channel", "room"));
        when(userApiClient.getUserProfiles(Set.of("sender", "missing")))
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
        when(channelAccessService.getAccessibleTextChannels("user")).thenReturn(java.util.Map.of());

        assertThatThrownBy(() -> messageHistoryFacade.getMessages(
            "user", "room", "channel", null, 50
        )).isInstanceOf(AccessDeniedException.class);
    }
}
