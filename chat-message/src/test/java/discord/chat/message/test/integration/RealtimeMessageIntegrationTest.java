package discord.chat.message.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import discord.chat.common.infrastructure.user.User;
import discord.chat.message.application.message.RealtimeMessageService;
import discord.chat.message.infrastructure.client.chatapi.AccessibleTextChannelResponse;
import discord.chat.message.infrastructure.message.ChatMessage;
import discord.chat.message.infrastructure.message.ChatMessageMongoRepository;
import discord.chat.message.infrastructure.websocket.WebSocketSessionAuthorizationRegistry;
import discord.chat.message.interfaces.message.ReceivedTextMessageResponse;
import discord.chat.message.test.BaseIntegrationTest;

class RealtimeMessageIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private RealtimeMessageService realtimeMessageService;

    @Autowired
    private WebSocketSessionAuthorizationRegistry sessionAuthorizationRegistry;

    @Autowired
    private ChatMessageMongoRepository chatMessageMongoRepository;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void sendStoresAndDeliversAuthorizedMessage() {
        String sessionId = "session";
        sessionAuthorizationRegistry.register(
            sessionId,
            Set.of(new AccessibleTextChannelResponse("room", "channel"))
        );
        Authentication authentication = authentication();

        realtimeMessageService.send(
            "room", "channel", "hello", authentication, sessionId
        );

        List<ChatMessage> storedMessages = chatMessageMongoRepository.findAll();
        assertThat(storedMessages).singleElement().satisfies(message -> {
            assertThat(message.getSenderId()).isEqualTo("sender");
            assertThat(message.getContent()).isEqualTo("hello");
        });

        ArgumentCaptor<ReceivedTextMessageResponse> responseCaptor =
            ArgumentCaptor.forClass(ReceivedTextMessageResponse.class);
        verify(messagingTemplate).convertAndSendToUser(
            eq("sender"), eq("/channel"), responseCaptor.capture()
        );
        assertThat(responseCaptor.getValue().getContent()).isEqualTo("hello");
    }

    @Test
    void sendRejectsUnauthorizedChannel() {
        assertThatThrownBy(() -> realtimeMessageService.send(
            "room", "channel", "hello", authentication(), "unknown-session"
        )).isInstanceOf(AccessDeniedException.class);
        assertThat(chatMessageMongoRepository.findAll()).isEmpty();
    }

    private Authentication authentication() {
        User user = new User(
            "sender", "Sender", "sender@example.com", null, "image.png"
        );
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }
}
