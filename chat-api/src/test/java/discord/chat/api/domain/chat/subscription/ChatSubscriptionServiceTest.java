package discord.chat.api.domain.chat.subscription;

import discord.chat.api.application.session.ChatSessionEvent;
import discord.chat.api.infrastructure.redis.ChatSessionEventPublisher;
import discord.chat.api.support.InstanceSetter;
import discord.chat.common.infrastructure.chat.room.ChatRoom;
import discord.chat.common.infrastructure.chat.room.ChatRoomMongoRepository;
import discord.chat.common.infrastructure.chat.room.ChatRoomType;
import discord.chat.common.infrastructure.chat.subsription.ChatSubscription;
import discord.chat.common.infrastructure.chat.subsription.ChatSubscriptMongoRepository;
import discord.chat.common.infrastructure.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatSubscriptionServiceTest {
    @Mock
    private ChatSubscriptMongoRepository chatSubscriptMongoRepository;
    @Mock
    private ChatRoomMongoRepository chatRoomMongoRepository;
    @Mock
    private ChatSessionEventPublisher chatSessionEventPublisher;
    @InjectMocks
    private ChatSubscriptionService chatSubscriptionService;

    private User dummyMember;
    private Authentication dummyMemberAuthentication;
    private ChatRoom dummyChatRoom;

    // Creates a member and a community room owned by someone else.
    @BeforeEach
    void createMemberAndChatRoom() throws Exception {
        User dummyOwner = new User("owner", "Owner", "owner@example.com", null, null);
        dummyMember = new User("member", "Member", "member@example.com", null, null);
        dummyMemberAuthentication = new UsernamePasswordAuthenticationToken(dummyMember, null, List.of());
        dummyChatRoom = new ChatRoom(dummyOwner, "Room", "room.png", ChatRoomType.COMMUNITY);
        InstanceSetter.setField(dummyChatRoom, "id", "room");
    }

    // Checks that joining saves the membership and publishes the join to the member and the room.
    @Test
    void savesMembershipAndPublishesJoin() throws Exception {
        when(chatRoomMongoRepository.findByIdAndType("room", ChatRoomType.COMMUNITY))
            .thenReturn(Optional.of(dummyChatRoom));
        when(chatSubscriptMongoRepository.findByUserAndChatRoom(dummyMember, dummyChatRoom))
            .thenReturn(Optional.empty());

        chatSubscriptionService.subscribe(dummyMemberAuthentication, "room");

        ChatSessionEvent join = ChatSessionEvent.join("member", "Member", null, "room");
        verify(chatSubscriptMongoRepository).save(any(ChatSubscription.class));
        verify(chatSessionEventPublisher).publishToUserAfterCommit(join);
        verify(chatSessionEventPublisher).publishToRoomAfterCommit(join);
    }

    // Checks that leaving deletes the membership and publishes the leave to the room.
    @Test
    void deletesMembershipAndPublishesLeave() throws Exception {
        ChatSubscription dummyChatSubscription = new ChatSubscription(dummyMember, dummyChatRoom);
        when(chatRoomMongoRepository.findById("room")).thenReturn(Optional.of(dummyChatRoom));
        when(chatSubscriptMongoRepository.findByUserAndChatRoom(dummyMember, dummyChatRoom))
            .thenReturn(Optional.of(dummyChatSubscription));

        chatSubscriptionService.unsubscribe(dummyMemberAuthentication, "room");

        verify(chatSubscriptMongoRepository).delete(dummyChatSubscription);
        verify(chatSessionEventPublisher).publishToRoomAfterCommit(
            ChatSessionEvent.leave("member", "Member", "room")
        );
    }
}
