package discord.chat.common.infrastructure.chat.directmessage;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Document(collection = "direct_messages")
public class DirectMessage {
    @Id
    private String id;

    @Field("participant_ids")
    private List<String> participantIds;

    public DirectMessage(String firstUserId, String secondUserId) {
        if (firstUserId.compareTo(secondUserId) < 0) {
            participantIds = List.of(firstUserId, secondUserId);
        } else {
            participantIds = List.of(secondUserId, firstUserId);
        }
        id = String.join(":", participantIds);
    }

    public boolean hasParticipant(String userId) {
        return participantIds.contains(userId);
    }

    public String getOtherParticipantId(String userId) {
        if (!hasParticipant(userId)) {
            throw new IllegalArgumentException("User is not a direct message participant");
        }
        if (participantIds.get(0).equals(userId)) {
            return participantIds.get(1);
        }
        return participantIds.get(0);
    }
}
