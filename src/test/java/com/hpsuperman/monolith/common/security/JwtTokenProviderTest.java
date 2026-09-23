package com.hpsuperman.monolith.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtTokenProvider")
class JwtTokenProviderTest {
    private static final String SECRET = "unit-test-secret-must-be-at-least-32-bytes-long";
    private static final String OTHER_SECRET = "another-unit-test-secret-different-value-32b";

    @Test
    @DisplayName("密钥为空直接失败")
    void rejectsBlankSecret() {
        JwtProperties properties = properties();
        properties.setSecret("  ");

        assertThatThrownBy(() -> new JwtTokenProvider(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.jwt.secret");
    }

    @Test
    @DisplayName("密钥不足 32 字节直接失败")
    void rejectsShortSecret() {
        JwtProperties properties = properties();
        properties.setSecret("too-short");

        assertThatThrownBy(() -> new JwtTokenProvider(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HS256");
    }

    @Test
    @DisplayName("访问令牌 TTL 非正直接失败——否则 Redis 的 SET 丢掉 EX，撤销标记永不过期")
    void rejectsNonPositiveAccessTtl() {
        JwtProperties properties = properties();
        properties.setAccessTokenTtl(Duration.ZERO);

        assertThatThrownBy(() -> new JwtTokenProvider(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.jwt.access-token-ttl");
    }

    @Test
    @DisplayName("刷新令牌 TTL 非正直接失败")
    void rejectsNonPositiveRefreshTtl() {
        JwtProperties properties = properties();
        properties.setRefreshTokenTtl(Duration.ofSeconds(-1));

        assertThatThrownBy(() -> new JwtTokenProvider(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.jwt.refresh-token-ttl");
    }

    @Test
    @DisplayName("签发的令牌类型、主体、角色正确，刷新令牌不带角色")
    void issuesTokensWithCorrectClaims() {
        JwtTokenProvider provider = new JwtTokenProvider(properties());

        JwtTokenProvider.IssuedTokens tokens = provider.issue(7L, "zhangsan", "张三", List.of("ADMIN", "USER"));

        var accessClaims = provider.parse(tokens.accessToken()).getPayload();
        assertThat(accessClaims.get(JwtTokenProvider.CLAIM_TYPE, String.class))
                .isEqualTo(JwtTokenProvider.TYPE_ACCESS);
        assertThat(provider.getUserId(accessClaims)).isEqualTo(7L);
        assertThat(rolesOf(accessClaims)).containsExactly("ADMIN", "USER");

        var refreshClaims = provider.parse(tokens.refreshToken()).getPayload();
        assertThat(refreshClaims.get(JwtTokenProvider.CLAIM_TYPE, String.class))
                .isEqualTo(JwtTokenProvider.TYPE_REFRESH);
        assertThat(refreshClaims.getId()).isEqualTo(tokens.refreshJti());

        assertThat(refreshClaims.get(JwtTokenProvider.CLAIM_ROLES)).isNull();
    }

    @Test
    @DisplayName("别家密钥签的令牌验不过")
    void rejectsTokenSignedByAnotherKey() {
        JwtProperties other = properties();
        other.setSecret(OTHER_SECRET);
        String foreign = new JwtTokenProvider(other).issue(7L, "zhangsan", null, List.of()).accessToken();

        JwtTokenProvider provider = new JwtTokenProvider(properties());
        assertThatThrownBy(() -> provider.parse(foreign)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("TTL 极小（正数）时签出的令牌已过期——测试用它来造真过期令牌")
    void tinyTtlProducesExpiredToken() {
        JwtProperties properties = properties();
        properties.setRefreshTokenTtl(Duration.ofMillis(1));

        JwtTokenProvider provider = new JwtTokenProvider(properties);
        String token = provider.issue(7L, "zhangsan", null, List.of()).refreshToken();

        assertThatThrownBy(() -> provider.parse(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @SuppressWarnings("unchecked")
    private static List<String> rolesOf(Claims claims) {
        return (List<String>) claims.get(JwtTokenProvider.CLAIM_ROLES, List.class);
    }

    private static JwtProperties properties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setIssuer("java-monolith");
        properties.setAccessTokenTtl(Duration.ofMinutes(30));
        properties.setRefreshTokenTtl(Duration.ofDays(7));
        return properties;
    }
}
