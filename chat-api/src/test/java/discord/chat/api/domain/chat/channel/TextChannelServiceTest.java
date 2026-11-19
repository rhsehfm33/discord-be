package discord.chat.api.domain.chat.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import discord.chat.api.application.session.TextChannelEvent;
import discord.chat.api.infrastructure.redis.ChatSessionEventPublisher;
import discord.chat.api.interfaces.chat.channel.TextChannelRequest;
import discord.chat.api.support.InstanceSetter;
import discord.chat.common.exception.CustomIllegalArgumentException;
import discord.chat.common.infrastructure.chat.channel.TextChannel;
import discord.chat.common.infrastructure.chat.channel.TextChannelMongoRepository;
import discord.chat.common.infrastructure.chat.room.ChatRoom;
import discord.chat.common.infrastructure.chat.room.ChatRoomMongoRepository;
import discord.chat.common.infrastructure.chat.room.ChatRoomType;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TextChannelServiceTest {
    @Mock
    private TextChannelMongoRepository textChannelMongoRepository;
    @Mock
    private ChatRoomMongoRepository chatRoomMongoRepository;
    @Mock
    private ChatSubscriptMongoRepository chatSubscriptMongoRepository;
    @Mock
    private ChatSessionEventPublisher chatSessionEventPublisher;
    @InjectMocks
    private TextChannelService textChannelService;

    private Authentication dummyOwnerAuthentication;
    private Authentication dummyOutsiderAuthentication;
    private TextChannel dummyTextChannel;

    // Creates an owner, an outsider, and a text channel in the owner's room.
    @BeforeEach
    void createOwnerAndTextChannel() throws Exception {
        User dummyOwner = new User("owner", "Owner", "owner@example.com", null, null);
        User dummyOutsider = new User("outsider", "Outsider", "outsider@example.com", null, null);
        dummyOwnerAuthentication = new UsernamePasswordAuthenticationToken(dummyOwner, null, List.of());
        dummyOutsiderAuthentication = new UsernamePasswordAuthenticationToken(dummyOutsider, null, List.of());
        ChatRoom dummyChatRoom = new ChatRoom(dummyOwner, "Room", "room.png", ChatRoomType.COMMUNITY);
        InstanceSetter.setField(dummyChatRoom, "id", "room");
        dummyTextChannel = new TextChannel("Topic", dummyOwner, dummyChatRoom);
        InstanceSetter.setField(dummyTextChannel, "id", "channel");
    }

    // Checks that the owner can rename a channel and the rename is published to the room.
    @Test
    void renamesTextChannelAndPublishesEvent() throws Exception {
        when(textChannelMongoRepository.findById("channel")).thenReturn(Optional.of(dummyTextChannel));
        when(textChannelMongoRepository.save(dummyTextChannel)).thenReturn(dummyTextChannel);

        String renamedTitle = textChannelService.update(dummyOwnerAuthentication, renameRequest()).getTitle();

        assertThat(renamedTitle).isEqualTo("Updated");
        verify(chatSessionEventPublisher).publishToRoomAfterCommit(new TextChannelEvent(
            TextChannelEvent.Type.TEXT_CHANNEL_UPDATED, "room", "channel", "Updated", "owner"
        ));
    }

    // Checks that a user who does not own the channel cannot rename it.
    @Test
    void rejectsRenameByNonOwner() throws Exception {
        when(textChannelMongoRepository.findById("channel")).thenReturn(Optional.of(dummyTextChannel));

        assertThatThrownBy(() -> textChannelService.update(dummyOutsiderAuthentication, renameRequest()))
            .isInstanceOf(CustomIllegalArgumentException.class);
        verify(textChannelMongoRepository, never()).save(any());
        verifyNoInteractions(chatSessionEventPublisher);
    }

    // Checks that a user who does not own the channel cannot delete it.
    @Test
    void rejectsDeletionByNonOwner() {
        when(textChannelMongoRepository.findById("channel")).thenReturn(Optional.of(dummyTextChannel));

        assertThatThrownBy(() -> textChannelService.delete(dummyOutsiderAuthentication, "channel"))
            .isInstanceOf(CustomIllegalArgumentException.class);
        verify(textChannelMongoRepository, never()).delete(any());
        verifyNoInteractions(chatSessionEventPublisher);
    }

    // Creates a request that renames the channel to "Updated".
    private TextChannelRequest renameRequest() throws Exception {
        return new ObjectMapper().readValue("{\"id\":\"channel\",\"title\":\"Updated\"}", TextChannelRequest.class);
    }
}
