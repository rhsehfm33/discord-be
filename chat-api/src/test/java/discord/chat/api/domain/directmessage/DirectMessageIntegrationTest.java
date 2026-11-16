package discord.chat.api.domain.directmessage;

import discord.chat.api.interfaces.directmessage.DirectMessageResponse;
import discord.chat.api.support.BaseIntegrationTest;
import discord.chat.common.infrastructure.chat.directmessage.DirectMessageMongoRepository;
import discord.chat.common.infrastructure.friend.friendship.FriendStatus;
import discord.chat.common.infrastructure.friend.friendship.Friendship;
import discord.chat.common.infrastructure.friend.friendship.FriendshipMongoRepository;
import discord.chat.common.infrastructure.user.User;
import discord.chat.common.infrastructure.user.UserMongoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DirectMessageIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private DirectMessageService directMessageService;
    @Autowired
    private DirectMessageMongoRepository directMessageMongoRepository;
    @Autowired
    private FriendshipMongoRepository friendshipMongoRepository;
    @Autowired
    private UserMongoRepository userMongoRepository;
    private Authentication aliceAuthentication;
    private Authentication bobAuthentication;
    private Friendship aliceFriendship;
    private Friendship bobFriendship;

    // Creates two users and their friendships for the direct message tests.
    @BeforeEach
    void createUsersAndFriendships() {
        aliceAuthentication = createUserAndBuildAuthentication("alice");
        bobAuthentication = createUserAndBuildAuthentication("bob");
        aliceFriendship = createFriendship(aliceAuthentication, bobAuthentication);
        bobFriendship = createFriendship(bobAuthentication, aliceAuthentication);
    }

    // Checks that opening a direct message creates its document.
    @Test
    void createsConversation() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(aliceAuthentication);
        DirectMessageResponse response = directMessageService.open(aliceAuthentication, aliceFriendship.getId());

        assertThat(directMessageMongoRepository.findById(response.id())).isPresent();
    }

    // Checks that opening the same conversation from the other side reuses its document.
    @Test
    void reusesConversationForBothFriends() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(aliceAuthentication);
        DirectMessageResponse aliceResponse = directMessageService.open(aliceAuthentication, aliceFriendship.getId());
        SecurityContextHolder.getContext().setAuthentication(bobAuthentication);
        DirectMessageResponse bobResponse = directMessageService.open(bobAuthentication, bobFriendship.getId());

        assertThat(bobResponse.id()).isEqualTo(aliceResponse.id());
        assertThat(directMessageMongoRepository.count()).isEqualTo(1);
    }

    // Checks that loading a conversation returns the other user's name.
    @Test
    void loadsConversationWithFriendName() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(aliceAuthentication);
        String directMessageId = directMessageService.open(aliceAuthentication, aliceFriendship.getId()).id();

        DirectMessageResponse response = directMessageService.get(aliceAuthentication, directMessageId);

        assertThat(response.id()).isEqualTo(directMessageId);
        assertThat(response.user().getNickName()).isEqualTo("bob");
    }

    // Clears authentication after every test.
    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    // Creates a persisted user and builds authentication for secured service calls.
    private Authentication createUserAndBuildAuthentication(String nickName) {
        User user = userMongoRepository.save(new User(nickName, nickName + "@example.com", "password", null));
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }

    // Creates one directional friendship between the two users.
    private Friendship createFriendship(Authentication fromAuthentication, Authentication toAuthentication) {
        User fromUser = (User) fromAuthentication.getPrincipal();
        User toUser = (User) toAuthentication.getPrincipal();
        return friendshipMongoRepository.save(new Friendship(fromUser, toUser, FriendStatus.FRIEND));
    }
}
