package discord.chat.message.infrastructure.client.chatapi;

import discord.chat.message.infrastructure.client.auth.InternalServiceTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Component
public class ChatApiClient {
    private final RestTemplate restTemplate;
    private final InternalServiceTokenProvider tokenProvider;
    private final String chatApiBaseUrl;

    public ChatApiClient(
        RestTemplate internalRestTemplate,
        InternalServiceTokenProvider tokenProvider,
        @Value("${internal.chat-api.base-url}") String chatApiBaseUrl
    ) {
        this.restTemplate = internalRestTemplate;
        this.tokenProvider = tokenProvider;
        this.chatApiBaseUrl = chatApiBaseUrl;
    }

    public List<AccessibleTextChannelResponse> getAccessibleTextChannels(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenProvider.getAccessToken());

        AccessibleTextChannelResponse[] channels = restTemplate.exchange(
            chatApiBaseUrl + "/internal/users/{userId}/text-channels",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            AccessibleTextChannelResponse[].class,
            userId
        ).getBody();

        return channels == null ? List.of() : Arrays.asList(channels);
    }

    public List<InternalUserProfileResponse> getUserProfiles(Collection<String> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenProvider.getAccessToken());

        InternalUserProfileResponse[] profiles = restTemplate.exchange(
            chatApiBaseUrl + "/internal/users/profiles",
            HttpMethod.POST,
            new HttpEntity<>(new InternalUserProfilesRequest(List.copyOf(userIds)), headers),
            InternalUserProfileResponse[].class
        ).getBody();

        return profiles == null ? List.of() : Arrays.asList(profiles);
    }
}
