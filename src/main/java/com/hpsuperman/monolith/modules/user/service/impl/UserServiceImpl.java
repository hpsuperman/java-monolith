package com.hpsuperman.monolith.modules.user.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.hpsuperman.monolith.common.enums.EnabledStatus;
import com.hpsuperman.monolith.common.enums.Role;
import com.hpsuperman.monolith.common.exception.BizException;
import com.hpsuperman.monolith.common.result.ResultCode;
import com.hpsuperman.monolith.common.security.JwtTokenProvider;
import com.hpsuperman.monolith.common.security.TokenStore;
import com.hpsuperman.monolith.modules.user.dto.*;
import com.hpsuperman.monolith.modules.user.entity.User;
import com.hpsuperman.monolith.modules.user.mapper.UserMapper;
import com.hpsuperman.monolith.modules.user.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenStore tokenStore;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TokenVO register(RegisterRequest request) {
        if (lambdaQuery().eq(User::getPhone, request.getPhone()).exists()) {
            throw new BizException(ResultCode.DATA_CONFLICT, "该手机号已注册");
        }
        User user = new User();
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname());
        user.setRoles(Role.USER.name());
        user.setStatus(EnabledStatus.ENABLED);

        save(user);
        return issueToken(user);
    }

    @Override
    public TokenVO login(LoginRequest request) {
        User user = lambdaQuery().eq(User::getPhone, request.getPhone()).one();
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException("手机号或密码错误");
        }
        if (!EnabledStatus.ENABLED.equals(user.getStatus())) {
            throw new BizException("账号已被禁用");
        }
        return issueToken(user);
    }


    @Override
    public TokenVO refresh(RefreshRequest request) {
        Claims claims = parseRefreshToken(request.getRefreshToken());
        Long userId = jwtTokenProvider.getUserId(claims);

        User user = getById(userId);
        if (user == null || !EnabledStatus.ENABLED.equals(user.getStatus())) {
            throw new BadCredentialsException("刷新令牌无效或已过期");
        }
        if (tokenStore.isRevoked(userId, claims.getId(), claims.getIssuedAt())) {
            throw new BadCredentialsException("刷新令牌无效或已过期");
        }
        JwtTokenProvider.IssuedTokens tokens = jwtTokenProvider.issue(
            user.getId(), user.getPhone(), Role.split(user.getRoles())
        );
        if (!tokenStore.rotateRefreshToken(userId, claims.getId(), tokens.refreshJti(), jwtTokenProvider.getRefreshTokenTtl())) {
            throw new BadCredentialsException("刷新令牌无效或已过期");
        }
        TokenVO vo = new TokenVO();
        vo.setAccessToken(tokens.accessToken());
        vo.setRefreshToken(tokens.refreshToken());
        return vo;
    }

    @Override
    public void logout(LogoutRequest request) {
        Claims claims;
        try {
            claims = jwtTokenProvider.parse(request.getRefreshToken()).getPayload();
        } catch (JwtException e) {
            return;
        }
        if (!JwtTokenProvider.TYPE_REFRESH.equals(
            claims.get(JwtTokenProvider.CLAIM_TYPE, String.class))) {
            return;
        }
        long userId = jwtTokenProvider.getUserId(claims);
        tokenStore.revokeUserTokens(userId);
        tokenStore.deleteRefreshTokenIfMatches(userId, claims.getId());

    }

    @Override
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        if (request.getNickname() == null && request.getEmail() == null && request.getAvatar() == null) {
            throw new BizException("没有需要修改的内容");
        }
        requireEnabledUser(userId);

        User user = new User();
        user.setId(userId);
        BeanUtils.copyProperties(request, user);
        if (!updateById(user)) {
            throw new BizException("用户不存在");
        }
    }
    @Override
    public UserVO getProfile(Long userId) {
        return UserConverter.toVO(requireEnabledUser(userId));
    }

    private User requireEnabledUser(Long userId) {
        User user = getById(userId);
        if (user == null || !EnabledStatus.ENABLED.equals(user.getStatus())) {
            throw new BadCredentialsException("账号不存在或已被禁用");
        }
        return user;
    }
    private TokenVO issueToken(User user) {
        JwtTokenProvider.IssuedTokens tokens = jwtTokenProvider.issue(user.getId(), user.getPhone(), Role.split(user.getRoles()));

        tokenStore.saveRefreshToken(user.getId(), tokens.refreshJti(), jwtTokenProvider.getRefreshTokenTtl());

        TokenVO vo = new TokenVO();
        vo.setAccessToken(tokens.accessToken());
        vo.setRefreshToken(tokens.refreshToken());

        return vo;
    }

    private Claims parseRefreshToken(String token) {
        try {
            Claims claims = jwtTokenProvider.parse(token).getPayload();
            if (!JwtTokenProvider.TYPE_REFRESH.equals(
                claims.get(JwtTokenProvider.CLAIM_TYPE, String.class))) {
                throw new BadCredentialsException("刷新令牌无效或已过期");
            }
            return claims;
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("刷新令牌无效或已过期");
        }
    }
}
