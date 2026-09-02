package discord.chat.api.interfaces.user;

import discord.chat.common.infrastructure.user.User;

public record InternalUserProfileResponse(String id, String nickName, String imageUrl) {
    public InternalUserProfileResponse(User user) {
        this(user.getId(), user.getNickName(), user.getImageUrl());
    }
}
