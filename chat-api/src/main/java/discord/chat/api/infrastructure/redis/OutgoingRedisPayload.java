package discord.chat.api.infrastructure.redis;

record OutgoingRedisPayload(RedisPayloadType type, Object data) {}
