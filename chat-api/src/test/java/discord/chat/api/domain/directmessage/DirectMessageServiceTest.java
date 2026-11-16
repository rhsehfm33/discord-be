package discord.chat.api.domain.directmessage;

import discord.chat.api.infrastructure.message.ChatMessage;
import discord.chat.api.infrastructure.message.ChatMessageRepository;
import discord.chat.api.infrastructure.redis.DirectMessageRedisBroker;
import discord.chat.api.interfaces.directmessage.DirectMessageMessageResponse;
import discord.chat.api.interfaces.message.MessageSenderResponse;
import discord.chat.api.support.InstanceSetter;
import discord.chat.common.exception.CustomEntityNotFoundException;
import discord.chat.common.infrastructure.chat.directmessage.DirectMessage;
import discord.chat.common.infrastructure.chat.directmessage.DirectMessageMongoRepository;
import discord.chat.common.infrastructure.friend.friendship.FriendshipMongoRepository;
import discord.chat.common.infrastructure.user.User;
import discord.chat.common.infrastructure.user.UserMongoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DirectMessageServiceTest {
    @Mock
    private DirectMessageMongoRepository directMessageMongoRepository;
    @Mock
    private FriendshipMongoRepository friendshipMongoRepository;
    @Mock
    private UserMongoRepository userMongoRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private DirectMessageRedisBroker directMessageRedisBroker;
    @InjectMocks
    private DirectMessageService directMessageService;

    private User dummyUser;
    private Authentication dummyAuthentication;
    private DirectMessage dummyDirectMessage;

    // Prepares a dummy user, authentication, and the DM returned by the repository.
    @BeforeEach
    void prepareDummyUserAuthenticationAndDirectMessage() {
        dummyUser = new User("sender", "Sender", "sender@example.com", null, "image.png");
        dummyAuthentication = new UsernamePasswordAuthenticationToken(dummyUser, null, List.of());
        dummyDirectMessage = new DirectMessage(dummyUser.getId(), "recipient");
        when(directMessageMongoRepository.findById(dummyDirectMessage.getId()))
            .thenReturn(Optional.of(dummyDirectMessage));
    }

    // Checks that a participant's DM is saved and published to both participants.
    @Test
    void saveAndPublishDirectMessage() throws Exception {
        ChatMessage dummyChatMessage = new ChatMessage(dummyUser.getId(), dummyDirectMessage.getId(), "hello");
        InstanceSetter.setField(dummyChatMessage, "id", "64b7a1");
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(dummyChatMessage);

        boolean saved = directMessageService.sendDirectMessage(
            dummyAuthentication, dummyDirectMessage.getId(), "hello"
        );

        // Verify the message passed to persistence.
        assertThat(saved).isTrue();
        ArgumentCaptor<ChatMessage> chatMessageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        var saveAndPublishOrder = inOrder(chatMessageRepository, directMessageRedisBroker);
        saveAndPublishOrder.verify(chatMessageRepository).save(chatMessageCaptor.capture());
        ChatMessage chatMessage = chatMessageCaptor.getValue();
        assertThat(chatMessage.getSenderId()).isEqualTo(dummyUser.getId());
        assertThat(chatMessage.getDirectMessageId()).isEqualTo(dummyDirectMessage.getId());
        assertThat(chatMessage.getContent()).isEqualTo("hello");

        // Verify the stored message is published to both participants after saving.
        DirectMessageMessageResponse dummyDirectMessageMessageResponse = new DirectMessageMessageResponse(
            "64b7a1", dummyDirectMessage.getId(),
            new MessageSenderResponse(dummyUser.getId(), dummyUser.getNickName(), dummyUser.getImageUrl()),
            "hello", dummyChatMessage.getCreatedAt()
        );
        saveAndPublishOrder.verify(directMessageRedisBroker).publish(
            dummyDirectMessageMessageResponse, dummyDirectMessage.getParticipantIds()
        );
    }

    // Checks that a non-participant cannot save or publish a DM.
    @Test
    void rejectDirectMessageFromNonParticipant() {
        User dummyOutsider = new User("outsider", "Outsider", "outsider@example.com", null, "image.png");
        var dummyOutsiderAuthentication = new UsernamePasswordAuthenticationToken(dummyOutsider, null, List.of());

        assertThatThrownBy(() -> directMessageService.sendDirectMessage(
            dummyOutsiderAuthentication, dummyDirectMessage.getId(), "hello"
        )).isInstanceOf(CustomEntityNotFoundException.class);

        verifyNoInteractions(chatMessageRepository, directMessageRedisBroker);
    }

    // Checks that a failed DM save returns false without publishing.
    @Test
    void skipDirectMessagePublicationWhenSaveFails() throws Exception {
        when(chatMessageRepository.save(any(ChatMessage.class)))
            .thenThrow(new DataAccessResourceFailureException("Database unavailable"));

        boolean saved = directMessageService.sendDirectMessage(
            dummyAuthentication, dummyDirectMessage.getId(), "hello"
        );

        assertThat(saved).isFalse();
        verify(chatMessageRepository).save(any(ChatMessage.class));
        verifyNoInteractions(directMessageRedisBroker);
    }
}
