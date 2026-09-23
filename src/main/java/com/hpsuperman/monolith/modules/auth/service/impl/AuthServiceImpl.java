package com.hpsuperman.monolith.modules.auth.service.impl;

import com.hpsuperman.monolith.common.enums.EnabledStatus;
import com.hpsuperman.monolith.common.exception.BizException;
import com.hpsuperman.monolith.common.security.JwtTokenProvider;
import com.hpsuperman.monolith.common.security.LoginUser;
import com.hpsuperman.monolith.common.security.SecurityUtils;
import com.hpsuperman.monolith.common.security.TokenStore;
import com.hpsuperman.monolith.modules.auth.dto.LoginRequest;
import com.hpsuperman.monolith.modules.auth.dto.LoginVO;
import com.hpsuperman.monolith.modules.auth.dto.RefreshTokenRequest;
import com.hpsuperman.monolith.modules.auth.service.AuthService;
import com.hpsuperman.monolith.modules.user.dto.UserConverter;
import com.hpsuperman.monolith.modules.user.dto.UserVO;
import com.hpsuperman.monolith.modules.user.entity.User;
import com.hpsuperman.monolith.modules.user.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final String TOKEN_TYPE = "Bearer";

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final TokenStore tokenStore;
    private final UserService userService;

    @Override
    public LoginVO login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (BadCredentialsException e) {
            log.warn("登录失败，凭证错误 | username={}", request.getUsername());
            throw new BizException("用户名或密码错误");
        } catch (DisabledException e) {
            log.warn("登录失败，账号已禁用 | username={}", request.getUsername());
            throw new BizException("账号已被禁用");
        } catch (AuthenticationException e) {
            log.warn("登录失败 | username={} | {}", request.getUsername(), e.getMessage());
            throw new BizException("用户名或密码错误");
        }

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        User user = userService.getById(loginUser.getUserId());
        BizException.requireNonNull(user, "用户不存在");

        log.info("登录成功 | userId={} | username={}", user.getId(), user.getUsername());

        JwtTokenProvider.IssuedTokens tokens = issue(user);

        tokenStore.saveRefreshToken(user.getId(), tokens.refreshJti(), tokenProvider.getRefreshTokenTtl());
        return toVO(user, tokens);
    }

    @Override
    public LoginVO refresh(RefreshTokenRequest request) {
        Claims claims = parseRefreshToken(request.getRefreshToken());

        if (!JwtTokenProvider.TYPE_REFRESH.equals(claims.get(JwtTokenProvider.CLAIM_TYPE, String.class))) {
            throw new BizException("传入的不是刷新令牌");
        }

        Long userId = tokenProvider.getUserId(claims);
        User user = userService.getById(userId);
        BizException.requireNonNull(user, "用户不存在");

        BizException.throwIf(user.getStatus() != EnabledStatus.ENABLED, "账号已被禁用");

        JwtTokenProvider.IssuedTokens tokens = issue(user);

        boolean rotated = tokenStore.rotateRefreshToken(
                userId, claims.getId(), tokens.refreshJti(), tokenProvider.getRefreshTokenTtl());
        BizException.throwIf(!rotated, "刷新令牌已失效，请重新登录");

        return toVO(user, tokens);
    }

    @Override
    public void logout(String refreshToken, String accessToken) {
        if (!StringUtils.hasText(refreshToken)) {
            log.debug("登出请求未携带 refreshToken，无可撤销的会话");
            return;
        }

        Claims claims;
        try {
            claims = tokenProvider.parse(refreshToken).getPayload();
        } catch (ExpiredJwtException e) {
            claims = e.getClaims();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("登出时刷新令牌无效，按成功处理: {}", e.getMessage());
            return;
        }

        if (!JwtTokenProvider.TYPE_REFRESH.equals(claims.get(JwtTokenProvider.CLAIM_TYPE, String.class))) {
            log.debug("登出时提交的不是刷新令牌，按成功处理");
            return;
        }

        Long userId;
        try {
            userId = tokenProvider.getUserId(claims);
        } catch (IllegalArgumentException e) {
            log.debug("登出时刷新令牌的 subject 非法，按成功处理");
            return;
        }

        boolean revokedSession = tokenStore.deleteRefreshTokenIfMatches(userId, claims.getId());

        blacklistAccessToken(accessToken);

        if (revokedSession) {
            log.info("登出成功 | userId={}", userId);
        } else {
            log.debug("登出时刷新令牌已轮换或已失效，无会话被撤销 | userId={}", userId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserVO currentUser() {
        Long userId = SecurityUtils.getUserId();
        User user = userService.getById(userId);
        BizException.requireNonNull(user, "用户不存在");
        return UserConverter.toVO(user);
    }

    private JwtTokenProvider.IssuedTokens issue(User user) {
        return tokenProvider.issue(
                user.getId(), user.getUsername(), user.getNickname(),
                UserConverter.parseRoles(user.getRoles()));
    }

    private LoginVO toVO(User user, JwtTokenProvider.IssuedTokens tokens) {
        return LoginVO.builder()
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .tokenType(TOKEN_TYPE)
                .expiresIn(tokenProvider.getAccessTokenTtl().toSeconds())
                .user(UserConverter.toVO(user))
                .build();
    }

    private void blacklistAccessToken(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            return;
        }
        try {
            Claims claims = tokenProvider.parse(accessToken).getPayload();
            Date expiration = claims.getExpiration();
            if (expiration != null) {
                tokenStore.blacklist(claims.getId(), Duration.between(Instant.now(), expiration.toInstant()));
            }
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("登出时访问令牌不可用，跳过黑名单: {}", e.getMessage());
        }
    }

    private Claims parseRefreshToken(String refreshToken) {
        try {
            return tokenProvider.parse(refreshToken).getPayload();
        } catch (ExpiredJwtException e) {
            throw new BizException("刷新令牌已过期，请重新登录");
        } catch (JwtException | IllegalArgumentException e) {
            throw new BizException("刷新令牌无效或已过期");
        }
    }
}
