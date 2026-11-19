package discord.chat.api.infrastructure.redis;

import com.fasterxml.jackson.databind.JsonNode;

record IncomingRedisPayload(RedisPayloadType type, JsonNode data) {}
