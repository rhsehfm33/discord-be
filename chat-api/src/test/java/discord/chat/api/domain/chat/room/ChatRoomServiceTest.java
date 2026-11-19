package discord.chat.api.domain.chat.room;

import com.fasterxml.jackson.databind.ObjectMapper;
import discord.chat.api.application.session.ChatSessionEvent;
import discord.chat.api.infrastructure.redis.ChatSessionEventPublisher;
import discord.chat.api.interfaces.chat.room.ChatRoomRequest;
import discord.chat.api.interfaces.chat.room.ChatRoomResponse;
import discord.chat.api.support.InstanceSetter;
import discord.chat.common.exception.CustomEntityNotFoundException;
import discord.chat.common.infrastructure.chat.channel.TextChannel;
import discord.chat.common.infrastructure.chat.channel.TextChannelMongoRepository;
import discord.chat.common.infrastructure.chat.room.ChatRoom;
import discord.chat.common.infrastructure.chat.room.ChatRoomMongoRepository;
import discord.chat.common.infrastructure.chat.room.ChatRoomType;
import discord.chat.common.infrastructure.chat.subsription.ChatSubscription;
import discord.chat.common.infrastructure.chat.subsription.ChatSubscriptMongoRepository;
import discord.chat.common.infrastructure.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {
    @Mock
    private ChatRoomMongoRepository chatRoomMongoRepository;
    @Mock
    private ChatSubscriptMongoRepository chatSubscriptMongoRepository;
    @Mock
    private TextChannelMongoRepository textChannelMongoRepository;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private ChatSessionEventPublisher chatSessionEventPublisher;
    @InjectMocks
    private ChatRoomService chatRoomService;

    private User dummyOwner;
    private Authentication dummyOwnerAuthentication;
    private ChatRoom dummyChatRoom;

    // Creates an owner and the owner's chat room.
    @BeforeEach
    void createOwnerAndChatRoom() throws Exception {
        dummyOwner = new User("owner", "Owner", "owner@example.com", null, null);
        dummyOwnerAuthentication = new UsernamePasswordAuthenticationToken(dummyOwner, null, List.of());
        dummyChatRoom = new ChatRoom(dummyOwner, "Room", "room.png", ChatRoomType.COMMUNITY);
        InstanceSetter.setField(dummyChatRoom, "id", "room");
    }

    // Checks that a new room saves the owner's membership and a general channel, then publishes the owner's join.
    @Test
    void createsRoomWithOwnerMembershipAndGeneralChannel() throws Exception {
        ChatRoomRequest chatRoomRequest = new ObjectMapper().readValue(
            "{\"title\":\"Room\",\"type\":\"COMMUNITY\",\"image\":\"room.png\"}", ChatRoomRequest.class
        );
        when(chatRoomMongoRepository.save(any(ChatRoom.class))).thenReturn(dummyChatRoom);

        ChatRoomResponse chatRoomResponse = chatRoomService.create(dummyOwnerAuthentication, chatRoomRequest);

        ArgumentCaptor<TextChannel> textChannelCaptor = ArgumentCaptor.forClass(TextChannel.class);
        verify(chatSubscriptMongoRepository).save(any(ChatSubscription.class));
        verify(textChannelMongoRepository).save(textChannelCaptor.capture());
        verify(chatSessionEventPublisher).publishToUserAfterCommit(
            ChatSessionEvent.join("owner", "Owner", null, "room")
        );
        assertThat(textChannelCaptor.getValue().getTitle()).isEqualTo("general");
        assertThat(chatRoomResponse.getId()).isEqualTo("room");
    }

    // Checks that deleting a room removes its memberships and channels, then publishes the deletion.
    @Test
    void deletesRoomWithItsMembershipsAndChannels() throws Exception {
        when(chatRoomMongoRepository.findByIdAndOwner("room", dummyOwner)).thenReturn(Optional.of(dummyChatRoom));

        chatRoomService.delete(dummyOwnerAuthentication, "room");

        verify(chatSubscriptMongoRepository).deleteAllByChatRoom(dummyChatRoom);
        verify(textChannelMongoRepository).deleteAllByChatRoom(dummyChatRoom);
        verify(chatRoomMongoRepository).deleteById("room");
        verify(chatSessionEventPublisher).publishToRoomAfterCommit(ChatSessionEvent.delete("room"));
    }

    // Checks that a user who does not own the room cannot delete it.
    @Test
    void rejectsRoomDeletionByNonOwner() {
        User dummyOutsider = new User("outsider", "Outsider", "outsider@example.com", null, null);
        var dummyOutsiderAuthentication = new UsernamePasswordAuthenticationToken(dummyOutsider, null, List.of());
        when(chatRoomMongoRepository.findByIdAndOwner("room", dummyOutsider)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatRoomService.delete(dummyOutsiderAuthentication, "room"))
            .isInstanceOf(CustomEntityNotFoundException.class);
        verify(chatRoomMongoRepository, never()).deleteById(any());
        verifyNoInteractions(chatSessionEventPublisher);
    }
}
