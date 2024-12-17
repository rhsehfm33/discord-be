package discord.chat.gateway.domain.user;

import java.util.Collections;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import discord.chat.gateway.infrastructure.chat.room.ChatRoom;
import discord.chat.gateway.infrastructure.chat.room.ChatRoomMongoRepository;
import discord.chat.gateway.infrastructure.chat.subsription.ChatSubscriptMongoRepository;
import discord.chat.gateway.infrastructure.chat.subsription.ChatSubscription;
import discord.chat.gateway.infrastructure.user.User;
import discord.chat.gateway.infrastructure.user.UserMongoRepository;
import discord.chat.gateway.interfaces.common.exception.CustomAuthorizationError;
import discord.chat.gateway.interfaces.common.exception.CustomEntityNotFoundException;
import discord.chat.gateway.interfaces.user.UserRequest;
import discord.chat.gateway.interfaces.user.UserResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserMongoRepository userMongoRepository;
    private final PasswordEncoder passwordEncoder;
    private final MongoTemplate mongoTemplate;
    private final ChatRoomMongoRepository chatRoomMongoRepository;
    private final ChatSubscriptMongoRepository chatSubscriptMongoRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userMongoRepository.findByEmail(email).orElseThrow(() ->
            new EntityNotFoundException("Wrong user info"));

        return new org.springframework.security.core.userdetails.User(
            user.getEmail(), user.getPassword(), Collections.emptyList());
    }

    public User getUserByEmail(String email) {
        return userMongoRepository.findByEmail(email).orElseThrow(() ->
            new EntityNotFoundException("Wrong user info"));
    }

    public UserResponse createdUser(UserRequest dto) {
        dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        User user = userMongoRepository.save(new User(dto));
        return new UserResponse(user);
    }

    @PreAuthorize("isAuthenticated()")
    public UserResponse getMyUserInfo(Authentication authentication) {
        return new UserResponse((User) authentication.getPrincipal());
    }

    @PreAuthorize("isAuthenticated()")
    public List<UserResponse> getParticipants(Authentication authentication, String chatRoomId) throws
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

        List<UserResponse> userResponses = chatSubscriptions.getMappedResults().stream()
            .map(chatSubscription -> UserResponse
                .builder()
                .id(chatSubscription.getUser().getId())
                .nickName(chatSubscription.getUser().getNickName())
                .build()
            )
            .toList();

        return userResponses;
    }

    public UserResponse updateUser(String id, UserRequest dto)
        throws CustomEntityNotFoundException {
        User targetUser = userMongoRepository.findById(id).orElseThrow(() ->
            new CustomEntityNotFoundException(null, "User not found with id : " + id));

        targetUser.setNickName(dto.getNickName());
        targetUser.setImageUrl(dto.getEmail());
        return new UserResponse(userMongoRepository.save(targetUser));
    }

    public void deleteUser(String id) {
        userMongoRepository.deleteById(id);
    }
}
