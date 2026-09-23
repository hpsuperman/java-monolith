package com.hpsuperman.monolith.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenStore")
class TokenStoreTest {
    private static final Duration ACCESS_TTL = Duration.ofMinutes(30);
    private static final Duration REFRESH_TTL = Duration.ofDays(7);
    private static final Duration REVOCATION_TTL = ACCESS_TTL.plus(Duration.ofMinutes(5));

    private static final Long USER_ID = 42L;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private TokenStore tokenStore;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setAccessTokenTtl(ACCESS_TTL);
        tokenStore = new TokenStore(redisTemplate, properties);
    }

    @Test
    @DisplayName("轮换时把旧 jti 换成新 jti，并把 TTL 作为第三个参数传给 Lua")
    void rotateRefreshToken_passesTtlAsThirdArgument() {
        when(redisTemplate.execute(ArgumentMatchers.<RedisScript<Long>>any(), eq(List.of("auth:refresh:42")),
                eq("old-jti"), eq("new-jti"), eq("604800")))
                .thenReturn(1L);

        assertThat(tokenStore.rotateRefreshToken(USER_ID, "old-jti", "new-jti", REFRESH_TTL)).isTrue();
    }

    @Test
    @DisplayName("轮换 CAS 未命中返回 false")
    void rotateRefreshToken_returnsFalseWhenNotMatched() {
        when(redisTemplate.execute(ArgumentMatchers.<RedisScript<Long>>any(), ArgumentMatchers.<List<String>>any(), any(), any(), any()))
                .thenReturn(0L);

        assertThat(tokenStore.rotateRefreshToken(USER_ID, "old-jti", "new-jti", REFRESH_TTL)).isFalse();
    }

    @Test
    @DisplayName("脚本返回 null 时返回 false 而不是 NPE")
    void rotateRefreshToken_handlesNullResult() {
        when(redisTemplate.execute(ArgumentMatchers.<RedisScript<Long>>any(), ArgumentMatchers.<List<String>>any(), any(), any(), any()))
                .thenReturn((Long) null);

        assertThat(tokenStore.rotateRefreshToken(USER_ID, "old-jti", "new-jti", REFRESH_TTL)).isFalse();
    }

    @Test
    @DisplayName("登出只在 jti 匹配时删除，且用 Lua 保证原子")
    void deleteRefreshTokenIfMatches_onlyDeletesMatchingJti() {
        when(redisTemplate.execute(ArgumentMatchers.<RedisScript<Long>>any(), eq(List.of("auth:refresh:42")), eq("jti-1")))
                .thenReturn(1L);

        assertThat(tokenStore.deleteRefreshTokenIfMatches(USER_ID, "jti-1")).isTrue();
    }

    @Test
    @DisplayName("撤销写入当前时间戳，TTL 比访问令牌长约 5 分钟")
    void revokeUserTokens_writesTimestampWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        long before = Instant.now().getEpochSecond();

        tokenStore.revokeUserTokens(USER_ID);

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("auth:revoked:user:42"), valueCaptor.capture(), eq(REVOCATION_TTL));

        long stored = Long.parseLong(valueCaptor.getValue());
        assertThat(stored).isBetween(before, Instant.now().getEpochSecond());
    }

    @Test
    @DisplayName("黑名单命中即撤销")
    void isRevoked_blacklistedJti() {
        stubMultiGet("1", null);

        assertThat(tokenStore.isRevoked(USER_ID, "jti-1", new Date())).isTrue();
    }

    @Test
    @DisplayName("签发时间不晚于撤销时刻的令牌被拒")
    void isRevoked_issuedAtNotAfterRevocation() {
        long revokedAt = Instant.now().getEpochSecond() - 100;
        stubMultiGet(null, String.valueOf(revokedAt));

        Date issuedAt = Date.from(Instant.ofEpochSecond(revokedAt - 60));
        assertThat(tokenStore.isRevoked(USER_ID, "jti-1", issuedAt)).isTrue();
    }

    @Test
    @DisplayName("撤销之后签发的令牌放行——重新启用后登录不该被自己的撤销标记挡住")
    void isRevoked_issuedAfterRevocation() {
        long revokedAt = Instant.now().getEpochSecond() - 100;
        stubMultiGet(null, String.valueOf(revokedAt));

        Date issuedAt = Date.from(Instant.ofEpochSecond(revokedAt + 60));
        assertThat(tokenStore.isRevoked(USER_ID, "jti-1", issuedAt)).isFalse();
    }

    @Test
    @DisplayName("既不在黑名单也没被撤销时放行")
    void isRevoked_neitherMarked() {
        stubMultiGet(null, null);

        assertThat(tokenStore.isRevoked(USER_ID, "jti-1", new Date())).isFalse();
    }

    @Test
    @DisplayName("MGET 整体返回 null 时按已撤销处理（fail-closed）")
    void isRevoked_failsClosedWhenMultiGetReturnsNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.multiGet(ArgumentMatchers.<List<String>>any())).thenReturn((List<String>) null);

        assertThat(tokenStore.isRevoked(USER_ID, "jti-1", new Date())).isTrue();
    }

    @Test
    @DisplayName("MGET 结果缺项时按已撤销处理（fail-closed）")
    void isRevoked_failsClosedWhenMultiGetIsShort() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.multiGet(ArgumentMatchers.<List<String>>any())).thenReturn(List.of("1"));

        assertThat(tokenStore.isRevoked(USER_ID, "jti-1", new Date())).isTrue();
    }

    @Test
    @DisplayName("撤销时间戳格式异常时按已撤销处理（fail-closed）")
    void isRevoked_failsClosedOnMalformedTimestamp() {
        stubMultiGet(null, "not-a-number");

        assertThat(tokenStore.isRevoked(USER_ID, "jti-1", new Date())).isTrue();
    }

    @Test
    @DisplayName("jti 为空时只查撤销键，不去拼 auth:blacklist:null")
    void isRevoked_withoutJtiSkipsBlacklist() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:revoked:user:42")).thenReturn(null);

        assertThat(tokenStore.isRevoked(USER_ID, null, new Date())).isFalse();

        verify(valueOperations, never()).multiGet(ArgumentMatchers.<List<String>>any());
    }

    @Test
    @DisplayName("剩余 TTL 非正时黑名单不写入")
    void blacklist_skipsNonPositiveTtl() {
        tokenStore.blacklist("jti-1", Duration.ZERO);
        tokenStore.blacklist("jti-1", Duration.ofSeconds(-1));
        tokenStore.blacklist(null, Duration.ofMinutes(1));

        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("黑名单 TTL 用令牌剩余有效期，过期即自动清理")
    void blacklist_usesRemainingTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        tokenStore.blacklist("jti-1", Duration.ofMinutes(3));

        verify(valueOperations).set("auth:blacklist:jti-1", "1", Duration.ofMinutes(3));
    }

    private void stubMultiGet(String blacklistValue, String revokedAt) {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.multiGet(List.of("auth:blacklist:jti-1", "auth:revoked:user:42")))
                .thenReturn(Arrays.asList(blacklistValue, revokedAt));
    }
}
