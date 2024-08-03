package team.discordbe.domain.user.services;

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

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import team.discordbe.domain.chat.subscription.model.ChatSubscription;
import team.discordbe.domain.user.dto.UserRequestDto;
import team.discordbe.domain.user.dto.UserResponseDto;
import team.discordbe.domain.user.model.User;
import team.discordbe.domain.user.repository.UserRepository;
import team.discordbe.global.exception.CustomEntityNotFoundException;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MongoTemplate mongoTemplate;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email).orElseThrow(() ->
            new EntityNotFoundException("Wrong user info"));

        return new org.springframework.security.core.userdetails.User(
            user.getEmail(), user.getPassword(), Collections.emptyList());
    }

    public UserResponseDto createdUser(UserRequestDto dto) {
        dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        User user = userRepository.save(new User(dto));
        return new UserResponseDto(user);
    }

    @PreAuthorize("isAuthenticated()")
    public UserResponseDto getMyUserInfo(Authentication authentication) {
        return new UserResponseDto((User) authentication.getPrincipal());
    }

    @PreAuthorize("isAuthenticated()")
    public List<UserResponseDto> getParticipants(Authentication authentication, String chatRoomId) {
        User user = (User) authentication.getPrincipal();

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

        List<UserResponseDto> userResponseDtos = chatSubscriptions.getMappedResults().stream()
            .map(chatSubscription -> UserResponseDto
                .builder()
                .id(chatSubscription.getUser().getId())
                .nickName(chatSubscription.getUser().getNickName())
                .build()
            )
            .toList();

        return userResponseDtos;
    }

    public UserResponseDto updateUser(String id, UserRequestDto dto)
        throws CustomEntityNotFoundException {
        User targetUser = userRepository.findById(id).orElseThrow(() ->
            new CustomEntityNotFoundException(null, "User not found with id : " + id));

        targetUser.setNickName(dto.getNickName());
        targetUser.setImageUrl(dto.getEmail());
        return new UserResponseDto(userRepository.save(targetUser));
    }

    public void deleteUser(String id) {
        userRepository.deleteById(id);
    }
}
