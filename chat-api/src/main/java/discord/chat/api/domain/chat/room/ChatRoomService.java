package discord.chat.api.domain.chat.room;

import java.util.List;
import java.util.Optional;

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

import lombok.RequiredArgsConstructor;
import discord.chat.common.infrastructure.chat.channel.TextChannel;
import discord.chat.common.infrastructure.chat.channel.TextChannelMongoRepository;
import discord.chat.common.infrastructure.chat.room.ChatRoom;
import discord.chat.common.infrastructure.chat.room.ChatRoomMongoRepository;
import discord.chat.common.infrastructure.chat.subsription.ChatSubscriptMongoRepository;
import discord.chat.common.infrastructure.chat.subsription.ChatSubscription;
import discord.chat.common.infrastructure.user.User;
import discord.chat.common.util.RandomImageUrlGenerator;
import discord.chat.api.interfaces.chat.room.ChatRoomRequest;
import discord.chat.api.interfaces.chat.room.ChatRoomResponse;
import discord.chat.common.exception.CustomEntityNotFoundException;

@Service
@Transactional
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ChatRoomService {
    private final ChatRoomMongoRepository chatRoomMongoRepository;
    private final ChatSubscriptMongoRepository chatSubscriptMongoRepository;
    private final TextChannelMongoRepository textChannelMongoRepository;
    private final MongoTemplate mongoTemplate;

    public ChatRoomResponse create(Authentication authentication, ChatRoomRequest chatRoomRequest) {
        if (chatRoomRequest.getImage() == null) {
            chatRoomRequest.setImage(RandomImageUrlGenerator.generate());
        }
        User owner = (User) authentication.getPrincipal();
        ChatRoom newChatRoom = new ChatRoom(
            owner, chatRoomRequest.getTitle(), chatRoomRequest.getImage(), chatRoomRequest.getType()
        );
        newChatRoom = chatRoomMongoRepository.save(newChatRoom);
        chatSubscriptMongoRepository.save(new ChatSubscription(owner, newChatRoom));
        TextChannel textChannel = new TextChannel("일반 채팅", owner, newChatRoom);
        textChannelMongoRepository.save(textChannel);
        return new ChatRoomResponse(newChatRoom, true);
    }

    public List<ChatRoomResponse> getAll(Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        MatchOperation matchOperation = Aggregation.match(
            Criteria.where("user.$id").is(new ObjectId(user.getId()))
        );
        LookupOperation lookupOperation = Aggregation.lookup(
            "chat_subscriptions", "chatRoom.$id", "_id", "chatRoomDetails"
        );
        Aggregation aggregation = Aggregation.newAggregation(matchOperation, lookupOperation);
        AggregationResults<ChatSubscription> chatSubscriptions = mongoTemplate.aggregate(
            aggregation, "chat_subscriptions", ChatSubscription.class
        );

        List<ChatRoomResponse> chatRoomResponses = chatSubscriptions.getMappedResults().stream()
            .map(chatSubscription -> new ChatRoomResponse(
                chatSubscription.getChatRoom(),
                chatSubscription.getChatRoom().getOwner().equals(user)
            ))
            .toList();
        return chatRoomResponses;
    }

    public ChatRoomResponse get(Authentication authentication, String chatRoomId)
        throws CustomEntityNotFoundException {
        User user = (User) authentication.getPrincipal();

        ChatRoom chatRoom = chatRoomMongoRepository.findById(chatRoomId).orElseThrow(
            () -> new CustomEntityNotFoundException("NOT_FOUND", "Chat room is not found under conditions")
        );
        return new ChatRoomResponse(chatRoom, chatRoom.getOwner().equals(user));
    }

    public ChatRoomResponse update(Authentication authentication, ChatRoomRequest chatRoomRequest)
        throws CustomEntityNotFoundException {
        User owner = (User) authentication.getPrincipal();
        String chatRoomId = chatRoomRequest.getId();
        ChatRoom chatRoom = chatRoomMongoRepository.findByIdAndOwner(chatRoomId, owner).orElseThrow(
            () -> new CustomEntityNotFoundException("NOT_FOUND", "Chat room is not found under conditions")
        );
        chatRoom.setTitle(chatRoomRequest.getTitle());
        return new ChatRoomResponse(chatRoomMongoRepository.save(chatRoom), true);
    }

    public void delete(Authentication authentication, String chatRoomId) throws CustomEntityNotFoundException {
        User owner = (User) authentication.getPrincipal();
        Optional<ChatRoom> chatRoomOptional = chatRoomMongoRepository.findByIdAndOwner(chatRoomId, owner);
        if (chatRoomOptional.isPresent()) {
            chatSubscriptMongoRepository.deleteAllByChatRoom(chatRoomOptional.get());
            textChannelMongoRepository.deleteAllByChatRoom(chatRoomOptional.get());
            chatRoomMongoRepository.deleteById(chatRoomId);
        } else {
            throw new CustomEntityNotFoundException("NOT_FOUND", "Chat room is not found under given conditions");
        }
    }
}
