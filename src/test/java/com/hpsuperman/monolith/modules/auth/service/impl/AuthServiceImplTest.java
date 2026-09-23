package com.hpsuperman.monolith.modules.auth.service.impl;

import com.hpsuperman.monolith.common.enums.EnabledStatus;
import com.hpsuperman.monolith.common.exception.BizException;
import com.hpsuperman.monolith.common.security.JwtProperties;
import com.hpsuperman.monolith.common.security.JwtTokenProvider;
import com.hpsuperman.monolith.common.security.LoginUser;
import com.hpsuperman.monolith.common.security.TokenStore;
import com.hpsuperman.monolith.modules.auth.dto.LoginRequest;
import com.hpsuperman.monolith.modules.auth.dto.LoginVO;
import com.hpsuperman.monolith.modules.auth.dto.RefreshTokenRequest;
import com.hpsuperman.monolith.modules.user.entity.User;
import com.hpsuperman.monolith.modules.user.service.UserService;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl")
class AuthServiceImplTest {
    private static final String SECRET = "unit-test-secret-must-be-at-least-32-bytes-long";
    private static final String ISSUER = "java-monolith";
    private static final Duration ACCESS_TTL = Duration.ofMinutes(30);
    private static final Duration REFRESH_TTL = Duration.ofDays(7);

    private static final Long USER_ID = 7L;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenStore tokenStore;

    @Mock
    private UserService userService;

    private JwtTokenProvider tokenProvider;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(properties(ACCESS_TTL, REFRESH_TTL));
        authService = new AuthServiceImpl(authenticationManager, tokenProvider, tokenStore, userService);
    }

    @Test
    @DisplayName("登录写刷新令牌用无条件覆盖，不用 CAS——否则「新登录挤掉旧登录」就没了")
    void login_overwritesRefreshToken() {
        when(authenticationManager.authenticate(any())).thenReturn(authenticationOf(USER_ID));
        when(userService.getById(USER_ID)).thenReturn(enabledUser());

        LoginVO response = authService.login(loginRequest());

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getUser().getId()).isEqualTo(USER_ID);

        ArgumentCaptor<String> jtiCaptor = ArgumentCaptor.forClass(String.class);
        verify(tokenStore).saveRefreshToken(eq(USER_ID), jtiCaptor.capture(), eq(REFRESH_TTL));

        assertThat(jtiCaptor.getValue()).isEqualTo(parseId(response.getRefreshToken()));
        verify(tokenStore, never()).rotateRefreshToken(anyLong(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("凭证错误统一提示「用户名或密码错误」，不区分用户是否存在")
    void login_mapsBadCredentialsToLoginFailed() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(loginRequest()))
                .isInstanceOf(BizException.class)
                .hasMessage("用户名或密码错误");
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("刷新成功时轮换到新 jti，且不回头再写一次刷新令牌")
    void refresh_rotatesToNewJti() {
        String oldRefresh = tokenProvider.issue(USER_ID, "zhangsan", "张三", List.of("USER")).refreshToken();
        when(userService.getById(USER_ID)).thenReturn(enabledUser());
        when(tokenStore.rotateRefreshToken(eq(USER_ID), anyString(), anyString(), eq(REFRESH_TTL)))
                .thenReturn(true);

        LoginVO response = authService.refresh(refreshRequest(oldRefresh));

        ArgumentCaptor<String> expectedJti = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> newJti = ArgumentCaptor.forClass(String.class);
        verify(tokenStore).rotateRefreshToken(eq(USER_ID), expectedJti.capture(), newJti.capture(), eq(REFRESH_TTL));
        assertThat(expectedJti.getValue()).isEqualTo(parseId(oldRefresh));
        assertThat(newJti.getValue()).isEqualTo(parseId(response.getRefreshToken()));

        verify(tokenStore, never()).saveRefreshToken(anyLong(), anyString(), any());
    }

    @Test
    @DisplayName("CAS 失败报刷新令牌已失效，且不删除任何刷新令牌")
    void refresh_rejectsWhenRotationLoses() {
        String oldRefresh = tokenProvider.issue(USER_ID, "zhangsan", "张三", List.of("USER")).refreshToken();
        when(userService.getById(USER_ID)).thenReturn(enabledUser());
        when(tokenStore.rotateRefreshToken(eq(USER_ID), anyString(), anyString(), any())).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(refreshRequest(oldRefresh)))
                .isInstanceOf(BizException.class)
                .hasMessage("刷新令牌已失效，请重新登录");

        verify(tokenStore, never()).deleteRefreshTokenIfMatches(anyLong(), anyString());
    }

    @Test
    @DisplayName("用户已禁用时先拒绝，不消费掉旧 jti")
    void refresh_rejectsDisabledUserBeforeCas() {
        String oldRefresh = tokenProvider.issue(USER_ID, "zhangsan", "张三", List.of("USER")).refreshToken();
        User disabled = enabledUser();
        disabled.setStatus(EnabledStatus.DISABLED);
        when(userService.getById(USER_ID)).thenReturn(disabled);

        assertThatThrownBy(() -> authService.refresh(refreshRequest(oldRefresh)))
                .isInstanceOf(BizException.class)
                .hasMessage("账号已被禁用");

        verify(tokenStore, never()).rotateRefreshToken(anyLong(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("刷新令牌已过期提示重新登录")
    void refresh_rejectsExpiredToken() {
        String expired = expiredRefreshToken();

        assertThatThrownBy(() -> authService.refresh(refreshRequest(expired)))
                .isInstanceOf(BizException.class)
                .hasMessage("刷新令牌已过期，请重新登录");
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("拿访问令牌冒充刷新令牌被拒")
    void refresh_rejectsAccessToken() {
        String accessToken = tokenProvider.issue(USER_ID, "zhangsan", "张三", List.of("USER")).accessToken();

        assertThatThrownBy(() -> authService.refresh(refreshRequest(accessToken)))
                .isInstanceOf(BizException.class)
                .hasMessage("传入的不是刷新令牌");
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("刷新令牌已过期时仍能撤销会话——这正是登出接口放开的理由")
    void logout_worksWithExpiredRefreshToken() {
        JwtTokenProvider.IssuedTokens tokens = expiredTokens();

        authService.logout(tokens.refreshToken(), null);

        verify(tokenStore).deleteRefreshTokenIfMatches(USER_ID, tokens.refreshJti());
    }

    @Test
    @DisplayName("登出同时把访问令牌的 jti 拉黑")
    void logout_blacklistsAccessToken() {
        JwtTokenProvider.IssuedTokens tokens =
                tokenProvider.issue(USER_ID, "zhangsan", "张三", List.of("USER"));

        authService.logout(tokens.refreshToken(), tokens.accessToken());

        verify(tokenStore).deleteRefreshTokenIfMatches(USER_ID, tokens.refreshJti());
        ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(tokenStore).blacklist(eq(parseId(tokens.accessToken())), ttl.capture());

        assertThat(ttl.getValue()).isPositive().isLessThanOrEqualTo(ACCESS_TTL);
    }

    @Test
    @DisplayName("未携带 refreshToken 时幂等成功，不碰 Redis")
    void logout_isIdempotentWithoutRefreshToken() {
        authService.logout(null, null);
        authService.logout("   ", null);

        verifyNoInteractions(tokenStore);
    }

    @Test
    @DisplayName("提交的不是刷新令牌时按成功处理，不删任何东西")
    void logout_ignoresNonRefreshToken() {
        JwtTokenProvider.IssuedTokens tokens =
                tokenProvider.issue(USER_ID, "zhangsan", "张三", List.of("USER"));

        authService.logout(tokens.accessToken(), tokens.accessToken());

        verify(tokenStore, never()).deleteRefreshTokenIfMatches(anyLong(), anyString());
    }

    @Test
    @DisplayName("乱码令牌按成功处理")
    void logout_ignoresGarbage() {
        authService.logout("not-a-jwt", null);

        verify(tokenStore, never()).deleteRefreshTokenIfMatches(anyLong(), anyString());
    }

    private static JwtProperties properties(Duration accessTtl, Duration refreshTtl) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setIssuer(ISSUER);
        properties.setAccessTokenTtl(accessTtl);
        properties.setRefreshTokenTtl(refreshTtl);
        return properties;
    }

    private JwtTokenProvider.IssuedTokens expiredTokens() {
        JwtTokenProvider shortLived = new JwtTokenProvider(properties(ACCESS_TTL, Duration.ofMillis(1)));
        JwtTokenProvider.IssuedTokens tokens =
                shortLived.issue(USER_ID, "zhangsan", "张三", List.of("USER"));

        assertThatThrownBy(() -> tokenProvider.parse(tokens.refreshToken()))
                .isInstanceOf(ExpiredJwtException.class);
        return tokens;
    }

    private String expiredRefreshToken() {
        return expiredTokens().refreshToken();
    }

    private static User enabledUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setUsername("zhangsan");
        user.setNickname("张三");
        user.setStatus(EnabledStatus.ENABLED);
        user.setRoles("USER");
        return user;
    }

    private static UsernamePasswordAuthenticationToken authenticationOf(Long userId) {
        LoginUser loginUser = new LoginUser(userId, "zhangsan", null, "张三",
                EnabledStatus.ENABLED, List.of("USER"));
        return new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
    }

    private static LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setUsername("zhangsan");
        request.setPassword("Passw0rd!");
        return request;
    }

    private static RefreshTokenRequest refreshRequest(String refreshToken) {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);
        return request;
    }

    private String parseId(String token) {
        return tokenProvider.parse(token).getPayload().getId();
    }
}
