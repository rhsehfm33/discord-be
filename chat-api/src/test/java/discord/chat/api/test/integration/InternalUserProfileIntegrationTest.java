package discord.chat.api.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import discord.chat.api.domain.user.InternalUserProfileService;
import discord.chat.api.interfaces.user.InternalUserProfileResponse;
import discord.chat.api.test.BaseIntegrationTest;
import discord.chat.common.exception.CustomIllegalArgumentException;
import discord.chat.common.infrastructure.user.User;
import discord.chat.common.infrastructure.user.UserMongoRepository;

class InternalUserProfileIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private InternalUserProfileService internalUserProfileService;

    @Autowired
    private UserMongoRepository userMongoRepository;

    @Test
    void getProfilesPreservesRequestedOrderAndOmitsMissingUsers() throws Exception {
        User first = userMongoRepository.save(
            new User("first", "first@example.com", "password", "first.png")
        );
        User second = userMongoRepository.save(
            new User("second", "second@example.com", "password", null)
        );

        List<InternalUserProfileResponse> result = internalUserProfileService.getProfiles(
            List.of(second.getId(), "000000000000000000000000", first.getId(), second.getId())
        );

        assertThat(result).containsExactly(
            new InternalUserProfileResponse(second.getId(), "second", null),
            new InternalUserProfileResponse(first.getId(), "first", "first.png")
        );
    }

    @Test
    void getProfilesRejectsInvalidRequests() {
        assertThatThrownBy(() -> internalUserProfileService.getProfiles(List.of()))
            .isInstanceOf(CustomIllegalArgumentException.class);
        assertThatThrownBy(() -> internalUserProfileService.getProfiles(List.of("user-1", " ")))
            .isInstanceOf(CustomIllegalArgumentException.class);
        assertThatThrownBy(() -> internalUserProfileService.getProfiles(
            java.util.stream.IntStream.rangeClosed(1, 101)
                .mapToObj(index -> "user-" + index)
                .toList()
        )).isInstanceOf(CustomIllegalArgumentException.class);
    }
}
