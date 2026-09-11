package discord.chat.api.domain.chat.room;

import java.util.List;
import discord.chat.api.interfaces.chat.room.ChatRoomParticipantResponse;
import discord.chat.common.exception.CustomAuthorizationError;
import discord.chat.common.exception.CustomEntityNotFoundException;
import discord.chat.common.infrastructure.chat.room.ChatRoom;
import discord.chat.common.infrastructure.chat.room.ChatRoomMongoRepository;
import discord.chat.common.infrastructure.chat.subsription.ChatSubscriptMongoRepository;
import discord.chat.common.infrastructure.chat.subsription.ChatSubscription;
import discord.chat.common.infrastructure.user.User;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatRoomParticipantService {
    private final MongoTemplate mongoTemplate;
    private final ChatRoomMongoRepository chatRoomMongoRepository;
    private final ChatSubscriptMongoRepository chatSubscriptMongoRepository;

    @PreAuthorize("isAuthenticated()")
    public List<ChatRoomParticipantResponse> getParticipants(Authentication authentication, String chatRoomId) throws
        CustomAuthorizationError, CustomEntityNotFoundException {
        User user = (User) authentication.getPrincipal();
        ChatRoom chatRoom = chatRoomMongoRepository.findById(chatRoomId).orElseThrow(
            () -> new CustomEntityNotFoundException("NOT_FOUND", "Chat room not found")
        );
        if (chatSubscriptMongoRepository.findByUserAndChatRoom(user, chatRoom).isEmpty()) {
            throw new CustomAuthorizationError("NO_AUTHORITY", "You're not subscribing the chat room");
        }

        MatchOperation matchOperation = Aggregation.match(
            Criteria.where("chatRoom.$id").is(new ObjectId(chatRoomId))
        );
        LookupOperation lookupOperation = Aggregation.lookup(
            "chat_subscriptions", "user.$id", "_id", "userDetails"
        );
        Aggregation aggregation = Aggregation.newAggregation(matchOperation, lookupOperation);
        AggregationResults<ChatSubscription> chatSubscriptions = mongoTemplate.aggregate(
            aggregation, "chat_subscriptions", ChatSubscription.class
        );

        List<ChatRoomParticipantResponse> userResponses = chatSubscriptions.getMappedResults().stream()
            .map(chatSubscription -> ChatRoomParticipantResponse
                .builder()
                .id(chatSubscription.getUser().getId())
                .nickName(chatSubscription.getUser().getNickName())
                .build()
            )
            .toList();

        return userResponses;
    }
}
