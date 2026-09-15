package discord.chat.api.application.session;

import discord.chat.api.domain.chat.channel.ChannelAccessService;
import discord.chat.api.infrastructure.websocket.ChatSessionRegistry;
import discord.chat.common.infrastructure.chat.channel.TextChannel;
import discord.chat.common.infrastructure.chat.room.ChatRoom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatSessionEventHandlerTest {
    @Mock
    private ChannelAccessService channelAccessService;
    @Mock
    private ChatSessionRegistry chatSessionRegistry;

    private ChatSessionEventHandler chatSessionEventHandler;

    // Creates the event handler with mocked dependencies.
    @BeforeEach
    void setUp() {
        chatSessionEventHandler = new ChatSessionEventHandler(channelAccessService, chatSessionRegistry);
    }

    // Checks that a join event loads channels belonging to the joined room.
    @Test
    void joinAccessibleRoom() {
        TextChannel joinedRoomChannel = createTextChannel("joined-room");
        when(chatSessionRegistry.hasUserSessions("user")).thenReturn(true);
        when(channelAccessService.getTextChannelsBy("user", "joined-room"))
            .thenReturn(List.of(joinedRoomChannel));

        chatSessionEventHandler.handle(ChatSessionEvent.join("user", "joined-room"));

        verify(chatSessionRegistry).join("user", List.of(joinedRoomChannel));
    }

    // Checks that a join event avoids a database lookup when the user is not connected locally.
    @Test
    void ignoreJoinForDisconnectedUser() {
        when(chatSessionRegistry.hasUserSessions("user")).thenReturn(false);

        chatSessionEventHandler.handle(ChatSessionEvent.join("user", "room"));

        verify(channelAccessService, never()).getTextChannelsBy("user", "room");
        verify(chatSessionRegistry, never()).join(anyString(), anyList());
    }

    // Checks that leave and delete events remove their corresponding cached access.
    @Test
    void removeCachedAccess() {
        chatSessionEventHandler.handle(ChatSessionEvent.leave("user", "room"));
        chatSessionEventHandler.handle(ChatSessionEvent.delete("room"));

        verify(chatSessionRegistry).leave("user", "room");
        verify(chatSessionRegistry).delete("room");
    }

    // Creates a text channel belonging to the requested room.
    private TextChannel createTextChannel(String chatRoomId) {
        TextChannel textChannel = mock(TextChannel.class);
        ChatRoom chatRoom = mock(ChatRoom.class);
        when(textChannel.getChatRoom()).thenReturn(chatRoom);
        when(chatRoom.getId()).thenReturn(chatRoomId);
        return textChannel;
    }
}
