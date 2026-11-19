package discord.chat.api.domain.chat.room;

import com.fasterxml.jackson.databind.ObjectMapper;
import discord.chat.api.application.session.ChatSessionEvent;
import discord.chat.api.application.session.TextChannelEvent;
import discord.chat.api.application.session.ChatSessionEventHandler;
import discord.chat.api.domain.chat.channel.TextChannelCache;
import discord.chat.api.domain.chat.channel.ChannelAccessService;
import discord.chat.api.domain.chat.channel.TextChannelService;
import discord.chat.api.domain.chat.subscription.ChatSubscriptionService;
import discord.chat.api.infrastructure.redis.ChatSessionEventPublisher;
import discord.chat.api.interfaces.chat.channel.TextChannelRequest;
import discord.chat.api.interfaces.chat.channel.TextChannelCreationRequest;
import discord.chat.api.interfaces.chat.room.ChatRoomRequest;
import discord.chat.api.support.BaseIntegrationTest;
import discord.chat.common.exception.CustomAuthorizationError;
import discord.chat.common.exception.CustomEntityNotFoundException;
import discord.chat.common.exception.CustomIllegalArgumentException;
import discord.chat.common.infrastructure.chat.channel.TextChannelMongoRepository;
import discord.chat.common.infrastructure.chat.room.ChatRoomMongoRepository;
import discord.chat.common.infrastructure.chat.subsription.ChatSubscriptMongoRepository;
import discord.chat.common.infrastructure.user.User;
import discord.chat.common.infrastructure.user.UserMongoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ChatRoomIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private ChatRoomService chatRoomService;
    @Autowired
    private ChatRoomParticipantService chatRoomParticipantService;
    @Autowired
    private ChatSubscriptionService chatSubscriptionService;
    @Autowired
    private TextChannelService textChannelService;
    @Autowired
    private TextChannelCache textChannelCache;
    @Autowired
    private ChatSessionEventHandler chatSessionEventHandler;
    @Autowired
    private ChannelAccessService channelAccessService;
    @Autowired
    private UserMongoRepository userMongoRepository;
    @Autowired
    private ChatRoomMongoRepository chatRoomMongoRepository;
    @Autowired
    private ChatSubscriptMongoRepository chatSubscriptMongoRepository;
    @Autowired
    private TextChannelMongoRepository textChannelMongoRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private ChatSessionEventPublisher chatSessionEventPublisher;

    private Authentication dummyOwner;
    private String dummyChatRoomId;

    // Creates an owner and their chat room before each test.
    @BeforeEach
    void createOwnerAndChatRoom() throws Exception {
        dummyOwner = createUserAndSetSecurityContext("owner");
        ChatRoomRequest chatRoomRequest = objectMapper.readValue(
            "{\"title\":\"Room\",\"type\":\"COMMUNITY\",\"image\":\"room.png\"}", ChatRoomRequest.class
        );
        dummyChatRoomId = chatRoomService.create(dummyOwner, chatRoomRequest).getId();
    }

    // Checks that a new chat room gives its owner access and one default text channel.
    @Test
    void createsChatRoomWithDefaultTextChannel() throws Exception {
        verify(chatSessionEventPublisher).publishToUserAfterCommit(
            ChatSessionEvent.join(dummyOwner.getName(), "owner", null, dummyChatRoomId)
        );
        assertThat(channelAccessService.getChatRoomIdsBy(dummyOwner.getName())).containsExactly(dummyChatRoomId);
        assertThat(textChannelService.getAllByChatRoom(dummyOwner, dummyChatRoomId)).hasSize(1);
    }

    // Checks that a joined member appears in the participant list and the join is published.
    @Test
    void addsMemberWhenJoiningChatRoom() throws Exception {
        Authentication dummyMember = createUserAndSetSecurityContext("member");
        chatSubscriptionService.subscribe(dummyMember, dummyChatRoomId);

        ChatSessionEvent joinEvent = ChatSessionEvent.join(dummyMember.getName(), "member", null, dummyChatRoomId);
        verify(chatSessionEventPublisher).publishToUserAfterCommit(joinEvent);
        verify(chatSessionEventPublisher).publishToRoomAfterCommit(joinEvent);
        assertThat(chatRoomParticipantService.getParticipants(dummyMember, dummyChatRoomId))
            .extracting("nickName").containsExactlyInAnyOrder("owner", "member");
    }

    // Checks that a member who leaves loses access to the chat room.
    @Test
    void removesAccessWhenLeavingChatRoom() throws Exception {
        Authentication dummyMember = createUserAndSetSecurityContext("member");
        chatSubscriptionService.subscribe(dummyMember, dummyChatRoomId);
        chatSubscriptionService.unsubscribe(dummyMember, dummyChatRoomId);

        verify(chatSessionEventPublisher).publishToRoomAfterCommit(
            ChatSessionEvent.leave(dummyMember.getName(), "member", dummyChatRoomId)
        );
        assertThatThrownBy(() -> chatRoomParticipantService.getParticipants(dummyMember, dummyChatRoomId))
            .isInstanceOf(CustomAuthorizationError.class);
    }

    // Checks that deleting a chat room also removes its memberships and text channels.
    @Test
    void deletesChatRoomWithItsData() throws Exception {
        chatRoomService.delete(dummyOwner, dummyChatRoomId);

        verify(chatSessionEventPublisher).publishToRoomAfterCommit(ChatSessionEvent.delete(dummyChatRoomId));
        assertThat(chatRoomMongoRepository.findById(dummyChatRoomId)).isEmpty();
        assertThat(chatSubscriptMongoRepository.count()).isZero();
        assertThat(textChannelMongoRepository.count()).isZero();
    }

    // Checks that a member can read the room's users, text channels, and room list through REST.
    @Test
    void servesChatRoomDataToMembersOverRest() throws Exception {
        Authentication dummyMember = createUserAndSetSecurityContext("member");
        chatSubscriptionService.subscribe(dummyMember, dummyChatRoomId);

        mockMvc.perform(get("/chat-rooms/{roomId}/users", dummyChatRoomId).with(authentication(dummyMember)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/chat-rooms/{roomId}/text-channels", dummyChatRoomId).with(authentication(dummyMember)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/chat-rooms").with(authentication(dummyMember)))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(dummyChatRoomId));
    }

    // Checks that creating a text channel publishes an event and links the channel to its room.
    @Test
    void createsTextChannel() throws Exception {
        String textChannelId = createTextChannel("Topic");

        verify(chatSessionEventPublisher).publishToRoomAfterCommit(
            new TextChannelEvent(TextChannelEvent.Type.TEXT_CHANNEL_CREATED, dummyChatRoomId, textChannelId)
        );
        assertThat(textChannelCache.belongsToRoom(textChannelId, dummyChatRoomId)).isTrue();
    }

    // Checks that renaming a text channel saves the new title and publishes an event.
    @Test
    void updatesTextChannel() throws Exception {
        String textChannelId = createTextChannel("Topic");
        TextChannelRequest textChannelRequest = objectMapper.readValue(
            "{\"id\":\"" + textChannelId + "\",\"title\":\"Updated\"}", TextChannelRequest.class
        );

        assertThat(textChannelService.update(dummyOwner, textChannelRequest).getTitle()).isEqualTo("Updated");
        verify(chatSessionEventPublisher).publishToRoomAfterCommit(
            new TextChannelEvent(TextChannelEvent.Type.TEXT_CHANNEL_UPDATED, dummyChatRoomId, textChannelId)
        );
    }

    // Checks that deleting a text channel removes it and publishes an event.
    @Test
    void deletesTextChannel() throws Exception {
        String textChannelId = createTextChannel("Topic");
        textChannelService.delete(dummyOwner, textChannelId);

        verify(chatSessionEventPublisher).publishToRoomAfterCommit(
            new TextChannelEvent(TextChannelEvent.Type.TEXT_CHANNEL_DELETED, dummyChatRoomId, textChannelId)
        );
        assertThat(textChannelMongoRepository.findById(textChannelId)).isEmpty();
    }

    // Checks that a deletion event removes a cached text channel.
    @Test
    void removesDeletedTextChannelFromCache() throws Exception {
        String textChannelId = createTextChannel("Topic");
        assertThat(textChannelCache.belongsToRoom(textChannelId, dummyChatRoomId)).isTrue();
        textChannelService.delete(dummyOwner, textChannelId);

        chatSessionEventHandler.handleTextChannelEvent(
            new TextChannelEvent(TextChannelEvent.Type.TEXT_CHANNEL_DELETED, dummyChatRoomId, textChannelId)
        );
        assertThat(textChannelCache.belongsToRoom(textChannelId, dummyChatRoomId)).isFalse();
    }

    // Checks that a member can read one text channel through REST.
    @Test
    void servesTextChannelToMembersOverRest() throws Exception {
        String textChannelId = createTextChannel("Topic");

        mockMvc.perform(get("/text-channels/{id}", textChannelId).with(authentication(dummyOwner)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Topic"));
    }

    // Checks that a non-member cannot read a text channel.
    @Test
    void hidesTextChannelFromNonMembers() throws Exception {
        String textChannelId = createTextChannel("Topic");
        Authentication dummyOutsider = createUserAndSetSecurityContext("outsider");

        assertThatThrownBy(() -> textChannelService.get(dummyOutsider, textChannelId))
            .isInstanceOf(CustomEntityNotFoundException.class);
    }

    // Checks that only the owner can delete a text channel or the chat room.
    @Test
    void rejectsDeletionByNonOwners() throws Exception {
        String textChannelId = createTextChannel("Topic");
        Authentication dummyOutsider = createUserAndSetSecurityContext("outsider");

        assertThatThrownBy(() -> textChannelService.delete(dummyOutsider, textChannelId))
            .isInstanceOf(CustomIllegalArgumentException.class);
        assertThatThrownBy(() -> chatRoomService.delete(dummyOutsider, dummyChatRoomId))
            .isInstanceOf(CustomEntityNotFoundException.class);
    }

    // Checks that the last text channel in a chat room cannot be deleted.
    @Test
    void keepsLastTextChannel() throws Exception {
        String defaultTextChannelId = textChannelService.getAllByChatRoom(dummyOwner, dummyChatRoomId).get(0).getId();

        assertThatThrownBy(() -> textChannelService.delete(dummyOwner, defaultTextChannelId))
            .isInstanceOf(CustomIllegalArgumentException.class)
            .hasMessage("A chat room must have at least one text channel.");
        assertThat(textChannelMongoRepository.existsById(defaultTextChannelId)).isTrue();
    }

    // Checks that the moved REST routes still require authentication.
    @Test
    void rejectsAnonymousRoomRequests() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/chat-rooms")).andExpect(status().isUnauthorized());
    }

    // Checks that a removed user cannot resolve channel access from a previously issued token.
    @Test
    void rejectsDeletedUsersWhenResolvingChannelAccess() {
        Authentication user = createUserAndSetSecurityContext("removed");
        userMongoRepository.deleteById(user.getName());
        assertThatThrownBy(() -> channelAccessService.getChatRoomIdsBy(user.getName()))
            .isInstanceOf(AccessDeniedException.class);
    }

    // Clears service authentication even when a test assertion fails.
    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    // Creates a persisted user and installs it as the current principal for secured service calls.
    private Authentication createUserAndSetSecurityContext(String nickName) {
        User user = userMongoRepository.save(new User(nickName, nickName + "@example.com", "password", null));
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }

    // Creates a text channel in the owner's chat room and returns its ID.
    private String createTextChannel(String title) throws Exception {
        TextChannelCreationRequest textChannelCreationRequest = objectMapper.readValue(
            "{\"title\":\"" + title + "\"}", TextChannelCreationRequest.class
        );
        return textChannelService.create(dummyOwner, dummyChatRoomId, textChannelCreationRequest).getId();
    }
}
