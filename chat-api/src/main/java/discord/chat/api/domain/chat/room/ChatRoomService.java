package discord.chat.api.domain.chat.room;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

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

import discord.chat.common.infrastructure.chat.channel.TextChannel;
import discord.chat.common.infrastructure.chat.channel.TextChannelMongoRepository;
import discord.chat.common.infrastructure.chat.room.ChatRoom;
import discord.chat.common.infrastructure.chat.room.ChatRoomMongoRepository;
import discord.chat.common.infrastructure.chat.subsription.ChatSubscriptMongoRepository;
import discord.chat.common.infrastructure.chat.subsription.ChatSubscription;
import discord.chat.common.infrastructure.user.User;
import discord.chat.api.interfaces.chat.room.ChatRoomRequest;
import discord.chat.api.interfaces.chat.room.ChatRoomResponse;
import discord.chat.common.exception.CustomEntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ChatRoomService {
    private final ChatRoomMongoRepository chatRoomMongoRepository;
    private final ChatSubscriptMongoRepository chatSubscriptMongoRepository;
    private final TextChannelMongoRepository textChannelMongoRepository;
    private final MongoTemplate mongoTemplate;

    private final Random random = new Random();
    List<String> dummyImages = new ArrayList<>(List.of(
        "https://i.pinimg.com/originals/a5/98/73/a598732adbce5c5f5c276474a5525330.jpg",
        "https://i.pinimg.com/474x/25/0f/2d/250f2d083b89d3b6585f67729602daaf.jpg",
        "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ3Vd_kqZn53ok20t0tVuAukGAHOzVLWvNgKw&s",
        "https://i.pinimg.com/564x/31/f1/23/31f1231af69beb6617062dbf3373131c.jpg"
    ));

    public ChatRoomResponse create(Authentication authentication, ChatRoomRequest chatRoomRequest) {
        // TODO: Apply user registered profile image
        if (chatRoomRequest.getImage() == null) {
            chatRoomRequest.setImage(dummyImages.get(random.nextInt(dummyImages.size())));
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
