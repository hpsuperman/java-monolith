package com.hpsuperman.monolith.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenStore {
    private static final String REFRESH_PREFIX = "auth:refresh:";
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";
    private static final String REVOKED_PREFIX = "auth:revoked:user:";

    private static final Duration REVOCATION_TTL_MARGIN = Duration.ofMinutes(5);

    private static final RedisScript<Long> ROTATE_REFRESH_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1])
            if current == ARGV[1] then
              redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[3])
              return 1
            end
            return 0
            """, Long.class);

    private static final RedisScript<Long> DELETE_IF_MATCHES_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1])
            if current == ARGV[1] then
              redis.call('DEL', KEYS[1])
              return 1
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;

    public void saveRefreshToken(Long userId, String jti, Duration ttl) {
        redisTemplate.opsForValue().set(REFRESH_PREFIX + userId, jti, ttl);
    }

    public boolean rotateRefreshToken(Long userId, String expectedJti, String newJti, Duration ttl) {
        return exec(ROTATE_REFRESH_SCRIPT, REFRESH_PREFIX + userId,
                expectedJti, newJti, String.valueOf(ttl.toSeconds()));
    }

    public boolean deleteRefreshTokenIfMatches(Long userId, String jti) {
        return exec(DELETE_IF_MATCHES_SCRIPT, REFRESH_PREFIX + userId, jti);
    }

    public void revokeUserTokens(Long userId) {
        Duration ttl = jwtProperties.getAccessTokenTtl().plus(REVOCATION_TTL_MARGIN);
        redisTemplate.opsForValue()
                .set(REVOKED_PREFIX + userId, String.valueOf(Instant.now().getEpochSecond()), ttl);
    }

    public boolean isRevoked(Long userId, String jti, Date issuedAt) {
        if (jti == null) {
            return isRevokedAt(getRevokedAt(userId), issuedAt);
        }

        List<String> keys = List.of(BLACKLIST_PREFIX + jti, REVOKED_PREFIX + userId);
        List<String> values = redisTemplate.opsForValue().multiGet(keys);
        if (values == null || values.size() < keys.size()) {
            log.error("读取令牌状态失败，按已撤销处理以保持 fail-closed | userId={}", userId);
            return true;
        }
        if (values.get(0) != null) {
            return true;
        }
        return isRevokedAt(values.get(1), issuedAt);
    }

    private String getRevokedAt(Long userId) {
        return redisTemplate.opsForValue().get(REVOKED_PREFIX + userId);
    }

    private boolean isRevokedAt(String revokedAtSeconds, Date issuedAt) {
        if (revokedAtSeconds == null) {
            return false;
        }
        if (issuedAt == null) {
            log.error("命中撤销名单但令牌缺少 iat，按已撤销处理 | revokedAt={}", revokedAtSeconds);
            return true;
        }
        try {
            long revokedAt = Long.parseLong(revokedAtSeconds);

            return issuedAt.getTime() / 1000L <= revokedAt;
        } catch (NumberFormatException e) {
            log.error("撤销时间戳格式异常，按已撤销处理 | value={}", revokedAtSeconds, e);
            return true;
        }
    }

    public void blacklist(String jti, Duration remainingTtl) {
        if (jti == null || remainingTtl == null || remainingTtl.isNegative() || remainingTtl.isZero()) {
            return;
        }
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, "1", remainingTtl);
    }

    private boolean exec(RedisScript<Long> script, String key, String... args) {
        Long result = redisTemplate.execute(script, List.of(key), (Object[]) args);

        return Long.valueOf(1L).equals(result);
    }
}
