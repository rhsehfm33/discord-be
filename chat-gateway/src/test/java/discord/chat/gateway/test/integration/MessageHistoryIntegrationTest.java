package discord.chat.gateway.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;

import discord.chat.gateway.application.message.MessageHistoryFacade;
import discord.chat.gateway.infrastructure.client.chatapi.AccessibleTextChannelResponse;
import discord.chat.gateway.infrastructure.client.chatapi.ChatApiClient;
import discord.chat.gateway.infrastructure.client.chatapi.InternalUserProfileResponse;
import discord.chat.gateway.infrastructure.client.message.ChatMessageClient;
import discord.chat.gateway.infrastructure.client.message.StoredMessageResponse;
import discord.chat.gateway.interfaces.chat.channel.ReceivedTextMessageResponse;
import discord.chat.gateway.test.BaseIntegrationTest;

class MessageHistoryIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private MessageHistoryFacade messageHistoryFacade;

    @MockBean
    private ChatApiClient chatApiClient;

    @MockBean
    private ChatMessageClient chatMessageClient;

    @Test
    void getMessagesAuthorizesAndAddsProfilesWithUnknownFallback() {
        when(chatApiClient.getAccessibleTextChannels("user"))
            .thenReturn(List.of(new AccessibleTextChannelResponse("room", "channel")));
        when(chatMessageClient.getMessages("room", "channel", null, 50)).thenReturn(List.of(
            new StoredMessageResponse(
                "message-1", "sender-1", "room", "channel", "hello", Instant.EPOCH
            ),
            new StoredMessageResponse(
                "message-2", "missing", "room", "channel", "world", Instant.EPOCH.plusSeconds(1)
            )
        ));
        when(chatApiClient.getUserProfiles(java.util.Set.of("sender-1", "missing")))
            .thenReturn(List.of(new InternalUserProfileResponse("sender-1", "Sender", "image.png")));

        List<ReceivedTextMessageResponse> result = messageHistoryFacade.getMessages(
            "user", "room", "channel", null, 50
        );

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSender().getNickName()).isEqualTo("Sender");
        assertThat(result.get(1).getSender().getNickName()).isEqualTo("Unknown");
        assertThat(result.get(1).getSender().getId()).isEqualTo("missing");
    }

    @Test
    void getMessagesRejectsUnauthorizedChannelsBeforeReadingMessages() {
        when(chatApiClient.getAccessibleTextChannels("user")).thenReturn(List.of());

        assertThatThrownBy(() -> messageHistoryFacade.getMessages(
            "user", "room", "channel", null, 50
        )).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(chatMessageClient);
    }
}
