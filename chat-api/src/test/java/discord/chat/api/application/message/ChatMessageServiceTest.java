package discord.chat.api.application.message;

import discord.chat.common.infrastructure.user.User;
import discord.chat.api.infrastructure.message.ChatMessage;
import discord.chat.api.infrastructure.message.ChatMessageRepository;
import discord.chat.api.infrastructure.redis.ChatMessageRedisBroker;
import discord.chat.api.infrastructure.websocket.ChatSessionRegistry;
import discord.chat.api.infrastructure.websocket.WebSocketSessionMessageSender;
import discord.chat.api.interfaces.message.ChatMessageResponse;
import discord.chat.api.interfaces.message.MessagePublishErrorResponse;
import discord.chat.api.support.InstanceSetter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {
    @Mock
    private ChatSessionRegistry chatSessionRegistry;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private WebSocketSessionMessageSender webSocketSessionMessageSender;
    @Mock
    private ChatMessageRedisBroker chatMessageRedisBroker;
    private ChatMessage storedMessage;

    private ChatMessageService chatMessageService;

    // Creates the service with mocked dependencies.
    @BeforeEach
    void setUp() {
        chatMessageService = new ChatMessageService(
                chatSessionRegistry, chatMessageRepository, webSocketSessionMessageSender, chatMessageRedisBroker
        );
    }

    // Checks that message is saved in DB and sent to Redis.
    @Test
    void publishStoredMessage() throws Exception {
        prepareStoredMessage();

        sendMessage();

        ArgumentCaptor<ChatMessageResponse> publishedMessageCaptor =
            ArgumentCaptor.forClass(ChatMessageResponse.class);
        ArgumentCaptor<ChatMessage> messageToSaveCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        var saveAndPublishOrder = inOrder(chatMessageRepository, chatMessageRedisBroker);
        saveAndPublishOrder.verify(chatMessageRepository).save(messageToSaveCaptor.capture());
        saveAndPublishOrder.verify(chatMessageRedisBroker).publish(publishedMessageCaptor.capture());

        // Verify the message passed to persistence.
        ChatMessage messageToSave = messageToSaveCaptor.getValue();
        assertThat(messageToSave.getSenderId()).isEqualTo("sender");
        assertThat(messageToSave.getChatRoomId()).isEqualTo("room");
        assertThat(messageToSave.getTextChannelId()).isEqualTo("channel");
        assertThat(messageToSave.getContent()).isEqualTo("hello");

        // Verify publication uses the stored message, including its generated ID.
        ChatMessageResponse publishedMessage = publishedMessageCaptor.getValue();
        assertThat(publishedMessage.getMessageId()).isEqualTo("64b7a1");
        assertThat(publishedMessage.getChatRoomId()).isEqualTo("room");
        assertThat(publishedMessage.getTextChannelId()).isEqualTo("channel");
        assertThat(publishedMessage.getContent()).isEqualTo("hello");
        assertThat(publishedMessage.getSender().id()).isEqualTo("sender");
        assertThat(publishedMessage.getCreatedAt()).isEqualTo(storedMessage.getCreatedAt());
        verifyNoInteractions(webSocketSessionMessageSender);
    }

    // Checks that a Redis failure sends an error to the sender.
    @Test
    void sendErrorToSendingSessionWhenPublicationFails() throws Exception {
        prepareStoredMessage();
        doThrow(new IllegalStateException("Redis unavailable")).when(chatMessageRedisBroker).publish(any());

        sendMessage();

        ArgumentCaptor<MessagePublishErrorResponse> publishErrorCaptor =
            ArgumentCaptor.forClass(MessagePublishErrorResponse.class);
        verify(webSocketSessionMessageSender).send(eq("session"), eq("/channel/errors"), publishErrorCaptor.capture());
        assertThat(publishErrorCaptor.getValue().messageId()).isEqualTo("64b7a1");
        assertThat(publishErrorCaptor.getValue().code()).isEqualTo("MESSAGE_PUBLISH_FAILED");
    }

    // Checks that a session without channel access cannot save or send messages.
    @Test
    void rejectSessionWithoutChannelAccess() {
        when(chatSessionRegistry.getAuthorizedChatRoomId("session", "channel")).thenReturn(Optional.empty());

        assertThatThrownBy(this::sendMessage).isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(chatMessageRepository, chatMessageRedisBroker, webSocketSessionMessageSender);
    }

    // Prepares channel access and the message returned by the repository.
    private void prepareStoredMessage() throws NoSuchFieldException, IllegalAccessException {
        storedMessage = new ChatMessage("sender", "room", "channel", "hello");
        InstanceSetter.setField(storedMessage, "id", "64b7a1");

        when(chatSessionRegistry.getAuthorizedChatRoomId("session", "channel")).thenReturn(Optional.of("room"));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(storedMessage);
    }

    // Sends a sample message as an authenticated user.
    private void sendMessage() {
        User sendingUser = new User("sender", "Sender", "sender@example.com", null, "image.png");
        var senderAuthentication = new UsernamePasswordAuthenticationToken(sendingUser, null, List.of());
        chatMessageService.send("channel", "hello", senderAuthentication, "session");
    }
}
