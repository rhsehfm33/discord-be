package discord.chat.api.integration;

import java.util.List;

import discord.chat.common.exception.CustomEntityNotFoundException;
import discord.chat.common.infrastructure.friend.friendship.FriendStatus;
import discord.chat.common.infrastructure.friend.friendship.FriendshipMongoRepository;
import discord.chat.common.infrastructure.friend.invitation.FriendInvitationMongoRepository;
import discord.chat.common.infrastructure.user.User;
import discord.chat.common.infrastructure.user.UserMongoRepository;
import discord.chat.api.domain.friend.friendship.FriendshipService;
import discord.chat.api.domain.friend.invitation.FriendInvitationService;
import discord.chat.api.interfaces.friend.friendship.FriendshipRequest;
import discord.chat.api.support.BaseIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class FriendIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private FriendInvitationService friendInvitationService;
    @Autowired
    private FriendshipService friendshipService;
    @Autowired
    private FriendInvitationMongoRepository friendInvitationMongoRepository;
    @Autowired
    private FriendshipMongoRepository friendshipMongoRepository;
    @Autowired
    private UserMongoRepository userMongoRepository;
    @Autowired
    private MockMvc mockMvc;
    private Authentication recipient;
    private Authentication sender;

    // Creates the two users needed by friend scenarios.
    @BeforeEach
    void createUsers() {
        recipient = createUserAndSetAuthentication("recipient");
        sender = createUserAndSetAuthentication("sender");
    }

    // Checks that accepting an invitation creates both directional friendship records.
    @Test
    void acceptsInvitationAndListsFriends() throws Exception {
        friendInvitationService.invite(sender, "recipient");
        String invitationId = friendInvitationMongoRepository.findAll().get(0).getId();
        assertThat(friendInvitationService.getSentInvitations(sender)).hasSize(1);

        SecurityContextHolder.getContext().setAuthentication(recipient);
        assertThat(friendInvitationService.getReceivedInvitations(recipient)).hasSize(1);
        friendInvitationService.accept(recipient, invitationId);
        assertThat(friendInvitationMongoRepository.count()).isZero();
        assertThat(friendshipMongoRepository.count()).isEqualTo(2);
        assertThat(friendshipService.getFriendshipStatus(recipient, "sender")).isEqualTo(FriendStatus.FRIEND);

        mockMvc.perform(get("/friends/friendships").param("friendStatus", "FRIEND")
                .with(authentication(recipient)))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].user.nickName").value("sender"));
    }

    // Checks that a user can change the status of their own friendship record.
    @Test
    void changesOwnFriendStatus() throws Exception {
        friendInvitationService.invite(sender, "recipient");
        String invitationId = friendInvitationMongoRepository.findAll().get(0).getId();
        SecurityContextHolder.getContext().setAuthentication(recipient);
        friendInvitationService.accept(recipient, invitationId);

        friendshipService.updateFriendship(recipient, new FriendshipRequest("sender", FriendStatus.BLOCKING));
        assertThat(friendshipService.getFriendsByStatus(recipient, FriendStatus.FRIEND)).isEmpty();
        assertThat(friendshipService.getFriendsByStatus(recipient, FriendStatus.BLOCKING)).hasSize(1);
        assertThat(friendshipService.getFriendsByStatus(sender, FriendStatus.FRIEND)).hasSize(1);
        assertThat(friendshipMongoRepository.count()).isEqualTo(2);

        friendshipService.updateFriendship(recipient, new FriendshipRequest("sender", FriendStatus.FRIEND));
        assertThat(friendshipService.getFriendsByStatus(recipient, FriendStatus.FRIEND)).hasSize(1);
    }

    // Checks that deleting a friendship removes both directional records.
    @Test
    void deletesFriendshipForBothUsers() throws Exception {
        friendInvitationService.invite(sender, "recipient");
        String invitationId = friendInvitationMongoRepository.findAll().get(0).getId();
        SecurityContextHolder.getContext().setAuthentication(recipient);
        friendInvitationService.accept(recipient, invitationId);
        assertThat(friendshipMongoRepository.count()).isEqualTo(2);
        String friendshipId = friendshipService.getFriendsByStatus(recipient, FriendStatus.FRIEND)
            .get(0).getFriendshipId();
        mockMvc.perform(delete("/friends/friendships/{friendshipId}", friendshipId)
                .with(authentication(recipient)))
            .andExpect(status().isOk());
        SecurityContextHolder.getContext().setAuthentication(recipient);
        assertThat(friendshipService.getFriendsByStatus(recipient, FriendStatus.FRIEND)).isEmpty();
        assertThat(friendshipService.getFriendsByStatus(sender, FriendStatus.FRIEND)).isEmpty();
        assertThat(friendshipMongoRepository.count()).isZero();
    }

    // Checks that only the recipient can accept an invitation and only the sender can cancel it.
    @Test
    void enforcesInvitationOwnership() throws Exception {
        friendInvitationService.invite(sender, "recipient");
        String invitationId = friendInvitationMongoRepository.findAll().get(0).getId();

        assertThatThrownBy(() -> friendInvitationService.accept(sender, invitationId))
            .isInstanceOf(CustomEntityNotFoundException.class);
        SecurityContextHolder.getContext().setAuthentication(recipient);
        assertThatThrownBy(() -> friendInvitationService.cancel(recipient, invitationId))
            .isInstanceOf(CustomEntityNotFoundException.class);
        SecurityContextHolder.getContext().setAuthentication(sender);
        friendInvitationService.cancel(sender, invitationId);
        assertThat(friendInvitationMongoRepository.count()).isZero();
        assertThat(friendshipMongoRepository.count()).isZero();
    }

    // Checks that anonymous clients cannot read friend or invitation lists.
    @Test
    void rejectsAnonymousFriendRequests() throws Exception {
        mockMvc.perform(get("/friends/friendships").param("friendStatus", "FRIEND").with(anonymous()))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/friends/invitations").param("isPassive", "true").with(anonymous()))
            .andExpect(status().isUnauthorized());
    }

    // Clears authentication after each test, including failed assertions.
    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    // Creates a persisted user and installs authentication for secured service calls.
    private Authentication createUserAndSetAuthentication(String nickName) {
        User user = userMongoRepository.save(new User(nickName, nickName + "@example.com", "password", null));
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }

}
