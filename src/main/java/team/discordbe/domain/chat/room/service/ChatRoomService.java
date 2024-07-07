package team.discordbe.domain.chat.room.service;

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
import team.discordbe.domain.chat.room.dto.ChatRoomRequestDto;
import team.discordbe.domain.chat.room.dto.ChatRoomResponseDto;
import team.discordbe.domain.chat.room.model.ChatRoom;
import team.discordbe.domain.chat.room.repository.ChatRoomRepository;
import team.discordbe.domain.chat.subscription.model.ChatSubscription;
import team.discordbe.domain.chat.subscription.repository.ChatSubscriptRepository;
import team.discordbe.domain.user.model.User;
import team.discordbe.global.exception.CustomEntityNotFoundException;

@Service
@Transactional
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatSubscriptRepository chatSubscriptRepository;
    private final MongoTemplate mongoTemplate;

    public ChatRoomResponseDto create(Authentication authentication, ChatRoomRequestDto chatRoomRequestDto) {
        User owner = (User) authentication.getPrincipal();
        ChatRoom newChatRoom = new ChatRoom(owner, chatRoomRequestDto);
        newChatRoom = chatRoomRepository.save(newChatRoom);
        chatSubscriptRepository.save(new ChatSubscription(owner, newChatRoom));
        return new ChatRoomResponseDto(newChatRoom);
    }

    public List<ChatRoomResponseDto> getAll(Authentication authentication) {
        User owner = (User) authentication.getPrincipal();

        MatchOperation matchOperation = Aggregation.match(
            Criteria.where("user.$id").is(new ObjectId(owner.getId()))
        );
        LookupOperation lookupOperation = Aggregation.lookup(
            "chat_subscriptions", "chatRoom.$id", "_id", "chatRoomDetails"
        );
        Aggregation aggregation = Aggregation.newAggregation(matchOperation, lookupOperation);
        AggregationResults<ChatSubscription> chatSubscriptions = mongoTemplate.aggregate(
            aggregation, "chat_subscriptions", ChatSubscription.class
        );

        List<ChatRoomResponseDto> chatRoomResponseDtos = chatSubscriptions.getMappedResults().stream()
            .map(chatSubscription -> new ChatRoomResponseDto(chatSubscription.getChatRoom()))
            .toList();
        return chatRoomResponseDtos;
    }

    public ChatRoomResponseDto update(Authentication authentication, ChatRoomRequestDto chatRoomRequestDto)
        throws CustomEntityNotFoundException {
        User owner = (User) authentication.getPrincipal();
        String chatRoomId = chatRoomRequestDto.getId();
        ChatRoom chatRoom = chatRoomRepository.findByIdAndOwner(chatRoomId, owner).orElseThrow(
            () -> new CustomEntityNotFoundException("NOT_FOUND", "Chat room is not found under conditions")
        );
        chatRoom.setTitle(chatRoomRequestDto.getTitle());
        return new ChatRoomResponseDto(chatRoomRepository.save(chatRoom));
    }

    public void delete(Authentication authentication, String chatRoomId) throws CustomEntityNotFoundException {
        User owner = (User) authentication.getPrincipal();
        Optional<ChatRoom> chatRoomOptional = chatRoomRepository.findByIdAndOwner(chatRoomId, owner);
        if (chatRoomOptional.isPresent()) {
            chatSubscriptRepository.deleteAllByChatRoom(chatRoomOptional.get());
            chatRoomRepository.deleteById(chatRoomId);
        } else {
            throw new CustomEntityNotFoundException("NOT_FOUND", "Chat room is not found under given conditions");
        }
    }
}
