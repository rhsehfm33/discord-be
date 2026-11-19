package discord.chat.api.domain.chat.room;

import discord.chat.api.support.InstanceSetter;
import discord.chat.common.exception.CustomAuthorizationError;
import discord.chat.common.infrastructure.chat.room.ChatRoom;
import discord.chat.common.infrastructure.chat.room.ChatRoomMongoRepository;
import discord.chat.common.infrastructure.chat.room.ChatRoomType;
import discord.chat.common.infrastructure.chat.subsription.ChatSubscriptMongoRepository;
import discord.chat.common.infrastructure.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomParticipantServiceTest {
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private ChatRoomMongoRepository chatRoomMongoRepository;
    @Mock
    private ChatSubscriptMongoRepository chatSubscriptMongoRepository;
    @InjectMocks
    private ChatRoomParticipantService chatRoomParticipantService;

    // Checks that a user outside the room cannot read its participants.
    @Test
    void rejectsParticipantListForNonMember() throws Exception {
        User dummyOwner = new User("owner", "Owner", "owner@example.com", null, null);
        User dummyOutsider = new User("outsider", "Outsider", "outsider@example.com", null, null);
        var dummyOutsiderAuthentication = new UsernamePasswordAuthenticationToken(dummyOutsider, null, List.of());
        ChatRoom dummyChatRoom = new ChatRoom(dummyOwner, "Room", "room.png", ChatRoomType.COMMUNITY);
        InstanceSetter.setField(dummyChatRoom, "id", "room");
        when(chatRoomMongoRepository.findById("room")).thenReturn(Optional.of(dummyChatRoom));
        when(chatSubscriptMongoRepository.findByUserAndChatRoom(dummyOutsider, dummyChatRoom))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatRoomParticipantService.getParticipants(dummyOutsiderAuthentication, "room"))
            .isInstanceOf(CustomAuthorizationError.class);
        verifyNoInteractions(mongoTemplate);
    }
}
