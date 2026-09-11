package discord.chat.message.integration;

import java.util.List;

import discord.chat.common.exception.CustomEntityNotFoundException;
import discord.chat.common.infrastructure.friend.friendship.FriendStatus;
import discord.chat.common.infrastructure.friend.friendship.FriendshipMongoRepository;
import discord.chat.common.infrastructure.friend.invitation.FriendInvitationMongoRepository;
import discord.chat.common.infrastructure.user.User;
import discord.chat.common.infrastructure.user.UserMongoRepository;
import discord.chat.message.domain.friend.friendship.FriendshipService;
import discord.chat.message.domain.friend.invitation.FriendInvitationService;
import discord.chat.message.interfaces.friend.friendship.FriendshipRequest;
import discord.chat.message.support.BaseIntegrationTest;
import org.junit.jupiter.api.AfterEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class FriendIntegrationTest extends BaseIntegrationTest {
    @Autowired private FriendInvitationService friendInvitationService;
    @Autowired private FriendshipService friendshipService;
    @Autowired private FriendInvitationMongoRepository friendInvitationMongoRepository;
    @Autowired private FriendshipMongoRepository friendshipMongoRepository;
    @Autowired private UserMongoRepository userMongoRepository;
    @Autowired private MockMvc mockMvc;

    // Checks invitation acceptance, profile responses, and blocking within the chat application.
    @Test
    void acceptsInvitationsAndChangesFriendStatus() throws Exception {
        Authentication recipient = authenticate("recipient");
        Authentication sender = authenticate("sender");
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
        SecurityContextHolder.getContext().setAuthentication(recipient);
        friendshipService.updateFriendship(recipient, new FriendshipRequest("sender", FriendStatus.BLOCKING));
        assertThat(friendshipService.getFriendsByStatus(recipient, FriendStatus.FRIEND)).isEmpty();
        assertThat(friendshipService.getFriendsByStatus(recipient, FriendStatus.BLOCKING)).hasSize(1);
    }

    // Checks that only the recipient can accept an invitation and only the sender can cancel it.
    @Test
    void enforcesInvitationOwnership() throws Exception {
        Authentication recipient = authenticate("recipient");
        Authentication sender = authenticate("sender");
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
        mockMvc.perform(get("/friends/friendships").param("friendStatus", "FRIEND"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/friends/invitations").param("isPassive", "true"))
            .andExpect(status().isUnauthorized());
    }

    // Clears authentication after each test, including failed assertions.
    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    // Creates a persisted user and installs authentication for secured service calls.
    private Authentication authenticate(String nickName) {
        User user = userMongoRepository.save(new User(nickName, nickName + "@example.com", "password", null));
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }
}
