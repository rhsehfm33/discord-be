package discord.chat.api.domain.user;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import discord.chat.api.interfaces.user.InternalUserProfileResponse;
import discord.chat.common.exception.CustomIllegalArgumentException;
import discord.chat.common.infrastructure.user.User;
import discord.chat.common.infrastructure.user.UserMongoRepository;
import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InternalUserProfileService {
    private static final int MAX_USER_IDS = 100;

    private final UserMongoRepository userMongoRepository;

    public List<InternalUserProfileResponse> getProfiles(List<String> userIds)
        throws CustomIllegalArgumentException {
        validate(userIds);

        LinkedHashSet<String> uniqueUserIds = new LinkedHashSet<>(userIds);
        Map<String, User> usersById = userMongoRepository.findAllById(uniqueUserIds).stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));

        return uniqueUserIds.stream()
            .map(usersById::get)
            .filter(user -> user != null)
            .map(InternalUserProfileResponse::new)
            .toList();
    }

    private void validate(List<String> userIds) throws CustomIllegalArgumentException {
        if (userIds == null || userIds.isEmpty()) {
            throw new CustomIllegalArgumentException(
                "INVALID_USER_IDS",
                "userIds must not be empty"
            );
        }
        if (userIds.size() > MAX_USER_IDS) {
            throw new CustomIllegalArgumentException(
                "INVALID_USER_IDS",
                "userIds must contain at most 100 values"
            );
        }
        if (userIds.stream().anyMatch(userId -> userId == null || userId.isBlank())) {
            throw new CustomIllegalArgumentException(
                "INVALID_USER_IDS",
                "userIds must not contain blank values"
            );
        }
    }
}
