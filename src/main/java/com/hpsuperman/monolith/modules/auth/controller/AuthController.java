package com.hpsuperman.monolith.modules.auth.controller;

import com.hpsuperman.monolith.common.result.Result;
import com.hpsuperman.monolith.common.security.BearerTokenResolver;
import com.hpsuperman.monolith.modules.auth.dto.LoginRequest;
import com.hpsuperman.monolith.modules.auth.dto.LoginVO;
import com.hpsuperman.monolith.modules.auth.dto.RefreshTokenRequest;
import com.hpsuperman.monolith.modules.auth.service.AuthService;
import com.hpsuperman.monolith.modules.user.dto.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证", description = "登录、令牌刷新、登出")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "登录", description = "返回 accessToken / refreshToken，无需鉴权")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @Operation(summary = "刷新令牌", description = "用 refreshToken 换取新的令牌对，旧刷新令牌立即失效")
    @PostMapping("/refresh")
    public Result<LoginVO> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.success(authService.refresh(request));
    }

    @Operation(summary = "登出",
            description = "用 refreshToken 作废该会话，并把当前访问令牌加入黑名单；幂等。"
                    + "必须提交 refreshToken——只凭访问令牌不足以注销一个会话，"
                    + "否则任何一份泄漏的（哪怕已过期的）访问令牌都能反复踢人下线。")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestBody(required = false) RefreshTokenRequest request,
                               HttpServletRequest httpRequest) {
        authService.logout(
                request == null ? null : request.getRefreshToken(),
                BearerTokenResolver.resolve(httpRequest));
        return Result.success();
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(authService.currentUser());
    }
}
