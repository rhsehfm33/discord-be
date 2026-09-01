package discord.chat.gateway.infrastructure.client.message;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import discord.chat.gateway.infrastructure.client.auth.InternalServiceTokenProvider;

@Component
public class ChatMessageClient {
    private final RestTemplate restTemplate;
    private final InternalServiceTokenProvider tokenProvider;
    private final String chatMessageBaseUrl;

    public ChatMessageClient(
        RestTemplate internalRestTemplate,
        InternalServiceTokenProvider tokenProvider,
        @Value("${internal.chat-message.base-url}") String chatMessageBaseUrl
    ) {
        this.restTemplate = internalRestTemplate;
        this.tokenProvider = tokenProvider;
        this.chatMessageBaseUrl = chatMessageBaseUrl;
    }

    public StoredMessageResponse createMessage(CreateStoredMessageRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenProvider.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        StoredMessageResponse response = restTemplate.postForObject(
            chatMessageBaseUrl + "/internal/messages",
            new HttpEntity<>(request, headers),
            StoredMessageResponse.class
        );
        if (response == null) {
            throw new IllegalStateException("Message service returned no stored message");
        }
        return response;
    }
}
