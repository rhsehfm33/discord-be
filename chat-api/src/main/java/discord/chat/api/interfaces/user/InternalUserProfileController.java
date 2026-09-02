package discord.chat.api.interfaces.user;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import discord.chat.api.domain.user.InternalUserProfileService;
import discord.chat.common.exception.CustomIllegalArgumentException;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserProfileController {
    private final InternalUserProfileService internalUserProfileService;

    @PostMapping("/profiles")
    @PreAuthorize("hasAuthority('SCOPE_internal')")
    public List<InternalUserProfileResponse> getProfiles(@RequestBody InternalUserProfilesRequest request)
        throws CustomIllegalArgumentException {
        return internalUserProfileService.getProfiles(request.userIds());
    }
}
