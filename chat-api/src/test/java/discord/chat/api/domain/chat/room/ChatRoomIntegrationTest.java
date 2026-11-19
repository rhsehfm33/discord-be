package discord.chat.api.domain.chat.room;

import com.fasterxml.jackson.databind.ObjectMapper;
import discord.chat.api.application.message.ChatMessageService;
import discord.chat.api.domain.chat.channel.TextChannelService;
import discord.chat.api.domain.chat.subscription.ChatSubscriptionService;
import discord.chat.api.infrastructure.websocket.ChatSessionRegistry;
import discord.chat.api.infrastructure.websocket.WebSocketUserMessageSender;
import discord.chat.api.interfaces.chat.room.ChatRoomEventResponse;
import discord.chat.api.interfaces.chat.room.ChatRoomRequest;
import discord.chat.api.interfaces.message.ChatMessageResponse;
import discord.chat.api.support.BaseIntegrationTest;
import discord.chat.common.infrastructure.user.User;
import discord.chat.common.infrastructure.user.UserMongoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

// Uses real MongoDB and Redis; only the final WebSocket transport is mocked.
class ChatRoomIntegrationTest extends BaseIntegrationTest {
    private static final Duration REDIS_DELIVERY_TIMEOUT = Duration.ofSeconds(5);

    @Autowired
    private ChatRoomService chatRoomService;
    @Autowired
    private ChatSubscriptionService chatSubscriptionService;
    @Autowired
    private TextChannelService textChannelService;
    @Autowired
    private ChatMessageService chatMessageService;
    @Autowired
    private ChatSessionRegistry chatSessionRegistry;
    @Autowired
    private UserMongoRepository userMongoRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private WebSocketUserMessageSender webSocketUserMessageSender;

    private Authentication dummyOwner;
    private Authentication dummyMember;
    private String dummyChatRoomId;
    private String dummyTextChannelId;

    // Creates an owner with a connected session, a room, and a member who is not yet connected.
    @BeforeEach
    void createRoomWithConnectedOwner() throws Exception {
        dummyOwner = createUser("owner");
        dummyMember = createUser("member");
        SecurityContextHolder.getContext().setAuthentication(dummyOwner);

        ChatRoomRequest chatRoomRequest = objectMapper.readValue(
            "{\"title\":\"Room\",\"type\":\"COMMUNITY\",\"image\":\"room.png\"}", ChatRoomRequest.class
        );
        dummyChatRoomId = chatRoomService.create(dummyOwner, chatRoomRequest).getId();
        dummyTextChannelId = textChannelService.getAllByChatRoom(dummyOwner, dummyChatRoomId).get(0).getId();
        chatSessionRegistry.register(dummyOwner.getName(), "owner-session");
    }

    // Removes the sessions so Redis subscriptions do not leak into other tests.
    @AfterEach
    void removeSessions() {
        chatSessionRegistry.removeSession(dummyOwner.getName(), "owner-session");
        chatSessionRegistry.removeSession(dummyMember.getName(), "member-session");
        SecurityContextHolder.clearContext();
    }

    // Checks that a member who connects after joining receives the owner's message through Redis.
    @Test
    void deliversMessageToConnectedMember() throws Exception {
        chatSubscriptionService.subscribe(dummyMember, dummyChatRoomId);
        chatSessionRegistry.register(dummyMember.getName(), "member-session");

        sendOwnerMessage("hello");

        assertThat(captureDeliveredMessage(dummyMember).getContent()).isEqualTo("hello");
    }

    // Checks that a connected member receives messages right after joining, without reconnecting.
    @Test
    void deliversMessageAfterJoiningWithoutReconnect() throws Exception {
        chatSessionRegistry.register(dummyMember.getName(), "member-session");
        chatSubscriptionService.subscribe(dummyMember, dummyChatRoomId);
        awaitRoomAccess(dummyMember, true);

        sendOwnerMessage("hello");

        assertThat(captureDeliveredMessage(dummyMember).getContent()).isEqualTo("hello");
    }

    // Checks that a member stops receiving messages after leaving the room.
    @Test
    void stopsDeliveryAfterLeaving() throws Exception {
        chatSubscriptionService.subscribe(dummyMember, dummyChatRoomId);
        chatSessionRegistry.register(dummyMember.getName(), "member-session");
        chatSubscriptionService.unsubscribe(dummyMember, dummyChatRoomId);
        awaitRoomAccess(dummyMember, false);

        sendOwnerMessage("hello");

        assertThat(captureDeliveredMessage(dummyOwner).getContent()).isEqualTo("hello");
        verify(webSocketUserMessageSender, never())
            .send(eq(dummyMember.getName()), eq("/channel"), any(ChatMessageResponse.class));
    }

    // Checks that a connected member is notified when the owner deletes the room.
    @Test
    void notifiesMemberWhenRoomIsDeleted() throws Exception {
        chatSubscriptionService.subscribe(dummyMember, dummyChatRoomId);
        chatSessionRegistry.register(dummyMember.getName(), "member-session");

        chatRoomService.delete(dummyOwner, dummyChatRoomId);

        verify(webSocketUserMessageSender, timeout(REDIS_DELIVERY_TIMEOUT.toMillis())).send(
            dummyMember.getName(), "/channel/events", ChatRoomEventResponse.deleted(dummyChatRoomId)
        );
    }

    // Creates a persisted user and returns its authentication without changing the security context.
    private Authentication createUser(String nickName) {
        User user = userMongoRepository.save(new User(nickName, nickName + "@example.com", "password", null));
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }

    // Sends a message from the owner to the room's default text channel.
    private void sendOwnerMessage(String content) {
        chatMessageService.send(dummyChatRoomId, dummyTextChannelId, content, dummyOwner);
    }

    // Waits until the session registry reflects the user's room access event from Redis.
    private void awaitRoomAccess(Authentication user, boolean expectedAccess) {
        await().atMost(REDIS_DELIVERY_TIMEOUT).untilAsserted(() ->
            assertThat(chatSessionRegistry.hasRoomAccess(user.getName(), dummyChatRoomId)).isEqualTo(expectedAccess)
        );
    }

    // Waits for a chat message delivered to the user and returns it.
    private ChatMessageResponse captureDeliveredMessage(Authentication user) {
        ArgumentCaptor<ChatMessageResponse> messageCaptor = ArgumentCaptor.forClass(ChatMessageResponse.class);
        verify(webSocketUserMessageSender, timeout(REDIS_DELIVERY_TIMEOUT.toMillis()))
            .send(eq(user.getName()), eq("/channel"), messageCaptor.capture());
        return messageCaptor.getValue();
    }
}
