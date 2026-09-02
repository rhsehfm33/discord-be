package discord.chat.gateway.infrastructure.client.message;

import discord.chat.gateway.infrastructure.client.auth.InternalServiceTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.Arrays;
import java.util.List;

@Component
public class ChatMessageClient {
    private static final String MESSAGES_PATH =
        "/internal/chat-rooms/{chatRoomId}/text-channels/{textChannelId}/messages";

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

    public StoredMessageResponse createMessage(
        String chatRoomId,
        String textChannelId,
        String senderId,
        String content
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenProvider.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        String url = chatMessageBaseUrl + MESSAGES_PATH;
        CreateMessageBody body = new CreateMessageBody(senderId, content);
        HttpEntity<CreateMessageBody> entity = new HttpEntity<>(body, headers);

        StoredMessageResponse response = restTemplate.postForObject(
            url,
            entity,
            StoredMessageResponse.class,
            chatRoomId,
            textChannelId
        );
        if (response == null) {
            throw new IllegalStateException("Message service returned no stored message");
        }
        return response;
    }

    public List<StoredMessageResponse> getMessages(
        String chatRoomId,
        String textChannelId,
        String beforeMessageId,
        int limit
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenProvider.getAccessToken());

        String url = chatMessageBaseUrl + MESSAGES_PATH + "?limit=" + limit;
        if (beforeMessageId != null && !beforeMessageId.isBlank()) {
            url += "&before=" + beforeMessageId;
        }

        StoredMessageResponse[] messages = restTemplate.exchange(
            url,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            StoredMessageResponse[].class,
            chatRoomId,
            textChannelId
        ).getBody();

        return messages == null ? List.of() : Arrays.asList(messages);
    }

    private record CreateMessageBody(String senderId, String content) {
    }
}
