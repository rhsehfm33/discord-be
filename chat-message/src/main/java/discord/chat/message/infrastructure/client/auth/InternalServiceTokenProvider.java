package discord.chat.message.infrastructure.client.auth;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class InternalServiceTokenProvider {
    private static final long EXPIRATION_SKEW_SECONDS = 30;

    private final RestTemplate restTemplate;
    private final String tokenUri;
    private final String clientId;
    private final String clientSecret;

    private String cachedAccessToken;
    private Instant refreshAt = Instant.EPOCH;

    public InternalServiceTokenProvider(
        RestTemplate internalRestTemplate,
        @Value("${internal.oauth.token-uri}") String tokenUri,
        @Value("${internal.oauth.client-id}") String clientId,
        @Value("${internal.oauth.client-secret}") String clientSecret
    ) {
        this.restTemplate = internalRestTemplate;
        this.tokenUri = tokenUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public synchronized String getAccessToken() {
        if (cachedAccessToken != null && Instant.now().isBefore(refreshAt)) {
            return cachedAccessToken;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("scope", "internal");

        OAuthTokenResponse response = restTemplate.postForObject(
            tokenUri,
            new HttpEntity<>(form, headers),
            OAuthTokenResponse.class
        );
        if (response == null || response.accessToken() == null) {
            throw new IllegalStateException("Authorization server returned no access token");
        }

        cachedAccessToken = response.accessToken();
        refreshAt = Instant.now().plusSeconds(
            Math.max(1, response.expiresIn() - EXPIRATION_SKEW_SECONDS)
        );
        return cachedAccessToken;
    }
}
