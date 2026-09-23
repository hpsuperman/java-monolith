package com.hpsuperman.monolith.modules.user.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.hpsuperman.monolith.common.enums.EnabledStatus;
import com.hpsuperman.monolith.common.enums.Role;
import com.hpsuperman.monolith.common.exception.BizException;
import com.hpsuperman.monolith.common.result.ResultCode;
import com.hpsuperman.monolith.common.security.JwtTokenProvider;
import com.hpsuperman.monolith.common.security.TokenStore;
import com.hpsuperman.monolith.modules.user.dto.LoginRequest;
import com.hpsuperman.monolith.modules.user.dto.RegisterRequest;
import com.hpsuperman.monolith.modules.user.dto.TokenVO;
import com.hpsuperman.monolith.modules.user.entity.User;
import com.hpsuperman.monolith.modules.user.mapper.UserMapper;
import com.hpsuperman.monolith.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenStore tokenStore;

    @Override
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

        save(user);
        return issueToken(user);
    }

    @Override
    public TokenVO login(LoginRequest request) {
        User user = lambdaQuery().eq(User::getPhone, request.getPhone()).one();
        if (user == null || !user.getPassword().equals(passwordEncoder.encode(request.getPassword()))) {
            throw new BizException("手机号或密码错误");
        }
        if (!user.getStatus().equals(EnabledStatus.ENABLED)) {
            throw new BizException("账号已被禁用");
        }
        return issueToken(user);
    }

    private TokenVO issueToken(User user) {
        JwtTokenProvider.IssuedTokens tokens = jwtTokenProvider.issue(user.getId(), user.getPhone(), user.getNickname(), Role.split(user.getRoles()));

        tokenStore.saveRefreshToken(user.getId(), tokens.refreshJti(), jwtTokenProvider.getRefreshTokenTtl());

        TokenVO vo = new TokenVO();
        vo.setAccessToken(tokens.accessToken());
        vo.setRefreshToken(tokens.refreshToken());

        return vo;
    }
}
