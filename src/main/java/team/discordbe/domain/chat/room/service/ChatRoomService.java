package team.discordbe.domain.chat.room.service;

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

import lombok.RequiredArgsConstructor;
import team.discordbe.domain.chat.channel.text.model.TextChannel;
import team.discordbe.domain.chat.channel.text.repository.TextChannelRepository;
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
    private final TextChannelRepository textChannelRepository;
    private final MongoTemplate mongoTemplate;

    private final Random random = new Random();
    List<String> dummyImages = new ArrayList<>(List.of(
        "https://i.pinimg.com/originals/a5/98/73/a598732adbce5c5f5c276474a5525330.jpg",
        "https://i.pinimg.com/474x/25/0f/2d/250f2d083b89d3b6585f67729602daaf.jpg",
        "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ3Vd_kqZn53ok20t0tVuAukGAHOzVLWvNgKw&s",
        "https://i.pinimg.com/564x/31/f1/23/31f1231af69beb6617062dbf3373131c.jpg"
    ));

    public ChatRoomResponseDto create(Authentication authentication, ChatRoomRequestDto chatRoomRequestDto) {
        // TODO: Apply user registered profile image
        if (chatRoomRequestDto.getImage() == null) {
            chatRoomRequestDto.setImage(dummyImages.get(random.nextInt(dummyImages.size())));
        }
        User owner = (User) authentication.getPrincipal();
        ChatRoom newChatRoom = new ChatRoom(owner, chatRoomRequestDto);
        newChatRoom = chatRoomRepository.save(newChatRoom);
        chatSubscriptRepository.save(new ChatSubscription(owner, newChatRoom));
        TextChannel textChannel = new TextChannel("일반 채팅", owner, newChatRoom);
        textChannel = textChannelRepository.save(textChannel);
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

    public ChatRoomResponseDto get(String chatRoomId)
        throws CustomEntityNotFoundException {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId).orElseThrow(
            () -> new CustomEntityNotFoundException("NOT_FOUND", "Chat room is not found under conditions")
        );
        return new ChatRoomResponseDto(chatRoom);
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
            textChannelRepository.deleteAllByChatRoom(chatRoomOptional.get());
            chatRoomRepository.deleteById(chatRoomId);
        } else {
            throw new CustomEntityNotFoundException("NOT_FOUND", "Chat room is not found under given conditions");
        }
    }
}
