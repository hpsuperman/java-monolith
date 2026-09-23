package com.hpsuperman.monolith.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtTokenProvider {
    public static final String CLAIM_TYPE = "type";
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_NICKNAME = "nickname";
    public static final String CLAIM_ROLES = "roles";

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private static final int MIN_SECRET_BYTES = 32;

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;

        if (properties.getSecret() == null || properties.getSecret().isBlank()) {
            throw new IllegalStateException("app.jwt.secret 未配置");
        }
        byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret 长度不足：当前 " + keyBytes.length + " 字节，HS256 要求至少 " + MIN_SECRET_BYTES + " 字节");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);

        requirePositive(properties.getAccessTokenTtl(), "app.jwt.access-token-ttl");
        requirePositive(properties.getRefreshTokenTtl(), "app.jwt.refresh-token-ttl");
    }

    private static void requirePositive(Duration ttl, String name) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalStateException(name + " 必须为正数，当前：" + ttl
                    + "。配成 0 或负数会让 Redis 的 SET 丢掉 EX，写出的 key 永不过期。");
        }
    }

    public record IssuedTokens(String accessToken, String refreshToken, String refreshJti) {
    }

    public IssuedTokens issue(Long userId, String username, String nickname, List<String> roles) {
        String refreshJti = UUID.randomUUID().toString();
        return new IssuedTokens(
                build(userId, username, nickname, roles, TYPE_ACCESS,
                        properties.getAccessTokenTtl(), UUID.randomUUID().toString()),
                build(userId, username, null, null, TYPE_REFRESH,
                        properties.getRefreshTokenTtl(), refreshJti),
                refreshJti);
    }

    private String build(Long userId, String username, String nickname,
                         List<String> roles, String type, Duration ttl, String jti) {
        Instant now = Instant.now();
        var builder = Jwts.builder()

                .id(jti)
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, type)
                .claim(CLAIM_USERNAME, username)
                .issuer(properties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)));

        if (nickname != null) {
            builder.claim(CLAIM_NICKNAME, nickname);
        }
        if (roles != null && !roles.isEmpty()) {
            builder.claim(CLAIM_ROLES, roles);
        }
        return builder.signWith(secretKey).compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(properties.getIssuer())
                .build()
                .parseSignedClaims(token);
    }

    public Long getUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public Duration getAccessTokenTtl() {
        return properties.getAccessTokenTtl();
    }

    public Duration getRefreshTokenTtl() {
        return properties.getRefreshTokenTtl();
    }
}
