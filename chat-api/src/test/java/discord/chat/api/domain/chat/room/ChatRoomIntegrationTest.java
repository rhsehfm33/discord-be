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

    // Checks room creation, membership changes, participant access, and room cleanup in the chat application.
    @Test
    void managesRoomsAndParticipants() throws Exception {
        Authentication owner = authenticate("owner");
        ChatRoomRequest request = objectMapper.readValue(
            "{\"title\":\"Room\",\"type\":\"COMMUNITY\",\"image\":\"room.png\"}", ChatRoomRequest.class
        );
        String chatRoomId = chatRoomService.create(owner, request).getId();
        verify(chatSessionEventPublisher).publishToUserAfterCommit(
            ChatSessionEvent.join(owner.getName(), "owner", null, chatRoomId)
        );
        assertThat(textChannelService.getAllByChatRoom(owner, chatRoomId)).hasSize(1);
        assertThat(channelAccessService.getChatRoomIdsBy(owner.getName()))
            .containsExactly(chatRoomId);

        Authentication member = authenticate("member");
        assertThat(channelAccessService.getChatRoomIdsBy(member.getName())).isEmpty();
        assertThatThrownBy(() -> chatRoomParticipantService.getParticipants(member, chatRoomId))
            .isInstanceOf(CustomAuthorizationError.class);
        chatSubscriptionService.subscribe(member, chatRoomId);
        ChatSessionEvent join = ChatSessionEvent.join(member.getName(), "member", null, chatRoomId);
        verify(chatSessionEventPublisher).publishToUserAfterCommit(join);
        verify(chatSessionEventPublisher).publishToRoomAfterCommit(join);
        assertThat(channelAccessService.getChatRoomIdsBy(member.getName()))
            .containsExactly(chatRoomId);
        assertThat(channelAccessService.hasChatRoomAccess(member.getName(), chatRoomId)).isTrue();
        assertThat(chatRoomParticipantService.getParticipants(member, chatRoomId))
            .extracting("nickName").containsExactlyInAnyOrder("owner", "member");

        mockMvc.perform(get("/chat-rooms/{roomId}/users", chatRoomId).with(authentication(member)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/chat-rooms/{roomId}/text-channels", chatRoomId).with(authentication(member)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/chat-rooms").with(authentication(member)))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(chatRoomId));

        SecurityContextHolder.getContext().setAuthentication(member);
        chatSubscriptionService.unsubscribe(member, chatRoomId);
        verify(chatSessionEventPublisher).publishToRoomAfterCommit(
            ChatSessionEvent.leave(member.getName(), "member", chatRoomId)
        );
        assertThat(channelAccessService.getChatRoomIdsBy(member.getName())).isEmpty();
        assertThat(channelAccessService.hasChatRoomAccess(member.getName(), chatRoomId)).isFalse();
        assertThatThrownBy(() -> chatRoomParticipantService.getParticipants(member, chatRoomId))
            .isInstanceOf(CustomAuthorizationError.class);

        SecurityContextHolder.getContext().setAuthentication(owner);
        chatRoomService.delete(owner, chatRoomId);
        verify(chatSessionEventPublisher).publishToRoomAfterCommit(ChatSessionEvent.delete(chatRoomId));
        assertThat(chatRoomMongoRepository.findById(chatRoomId)).isEmpty();
        assertThat(chatSubscriptMongoRepository.count()).isZero();
        assertThat(textChannelMongoRepository.count()).isZero();
        SecurityContextHolder.clearContext();
    }

    // Checks channel updates and owner restrictions after moving channel management into the chat application.
    @Test
    void managesChannelsAndRejectsOtherOwners() throws Exception {
        Authentication owner = authenticate("owner");
        ChatRoomRequest roomRequest = objectMapper.readValue(
            "{\"title\":\"Room\",\"type\":\"COMMUNITY\",\"image\":\"room.png\"}", ChatRoomRequest.class
        );
        String chatRoomId = chatRoomService.create(owner, roomRequest).getId();
        TextChannelCreationRequest textChannelCreationRequest = objectMapper.readValue(
            "{\"title\":\"Topic\"}", TextChannelCreationRequest.class
        );
        String textChannelId = textChannelService.create(owner, chatRoomId, textChannelCreationRequest).getId();
        assertThat(textChannelCache.belongsToRoom(textChannelId, chatRoomId)).isTrue();
        assertThat(textChannelCache.belongsToRoom(textChannelId, "other-room")).isFalse();
        verify(chatSessionEventPublisher).publishToRoomAfterCommit(
            new TextChannelEvent(TextChannelEvent.Type.TEXT_CHANNEL_CREATED, chatRoomId, textChannelId)
        );
        TextChannelRequest updateRequest = objectMapper.readValue(
            "{\"id\":\"" + textChannelId + "\",\"title\":\"Updated\"}", TextChannelRequest.class
        );
        assertThat(textChannelService.update(owner, updateRequest).getTitle()).isEqualTo("Updated");
        verify(chatSessionEventPublisher).publishToRoomAfterCommit(
            new TextChannelEvent(TextChannelEvent.Type.TEXT_CHANNEL_UPDATED, chatRoomId, textChannelId)
        );

        mockMvc.perform(get("/text-channels/{id}", textChannelId).with(authentication(owner)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Updated"));
        Authentication otherUser = authenticate("other");
        assertThatThrownBy(() -> textChannelService.get(otherUser, textChannelId))
            .isInstanceOf(CustomEntityNotFoundException.class);
        assertThatThrownBy(() -> textChannelService.delete(otherUser, textChannelId))
            .isInstanceOf(CustomIllegalArgumentException.class);
        assertThatThrownBy(() -> chatRoomService.delete(otherUser, chatRoomId))
            .isInstanceOf(CustomEntityNotFoundException.class);

        SecurityContextHolder.getContext().setAuthentication(owner);
        textChannelService.delete(owner, textChannelId);
        verify(chatSessionEventPublisher).publishToRoomAfterCommit(
            new TextChannelEvent(TextChannelEvent.Type.TEXT_CHANNEL_DELETED, chatRoomId, textChannelId)
        );
        assertThat(textChannelMongoRepository.findById(textChannelId)).isEmpty();
        chatSessionEventHandler.handleTextChannelEvent(
            new TextChannelEvent(TextChannelEvent.Type.TEXT_CHANNEL_DELETED, chatRoomId, textChannelId)
        );
        assertThat(textChannelCache.belongsToRoom(textChannelId, chatRoomId)).isFalse();

        String lastChannelId = textChannelService.getAllByChatRoom(owner, chatRoomId).get(0).getId();
        assertThatThrownBy(() -> textChannelService.delete(owner, lastChannelId))
            .isInstanceOf(CustomIllegalArgumentException.class)
            .hasMessage("A chat room must have at least one text channel.");
        assertThat(textChannelMongoRepository.existsById(lastChannelId)).isTrue();
        SecurityContextHolder.clearContext();
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
        Authentication user = authenticate("removed");
        userMongoRepository.deleteById(user.getName());
        assertThatThrownBy(() -> channelAccessService.getChatRoomIdsBy(user.getName()))
            .isInstanceOf(AccessDeniedException.class);
    }

    // Clears service authentication even when a test assertion fails.
    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    // Creates a persisted user and installs domain authentication for secured service calls.
    private Authentication authenticate(String nickName) {
        User user = userMongoRepository.save(new User(nickName, nickName + "@example.com", "password", null));
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }
}
