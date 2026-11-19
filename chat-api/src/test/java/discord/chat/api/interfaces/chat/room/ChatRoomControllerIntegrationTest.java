package discord.chat.api.interfaces.chat.room;

import discord.chat.api.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ChatRoomControllerIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    // Checks that room routes reject requests without authentication.
    @Test
    void rejectsAnonymousRoomRequests() throws Exception {
        mockMvc.perform(get("/chat-rooms")).andExpect(status().isUnauthorized());
    }
}
