package discord.chat.api.interfaces.directmessage;

import discord.chat.api.interfaces.friend.SimpleUserResponseDto;

public record DirectMessageResponse(String id, SimpleUserResponseDto user) {
}
