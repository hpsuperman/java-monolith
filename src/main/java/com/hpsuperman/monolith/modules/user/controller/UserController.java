package com.hpsuperman.monolith.modules.user.controller;

import com.hpsuperman.monolith.common.result.Result;
import com.hpsuperman.monolith.common.security.SecurityUtils;
import com.hpsuperman.monolith.modules.user.dto.*;
import com.hpsuperman.monolith.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @Operation(summary = "注册")
    @PostMapping("/register")
    public Result<TokenVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(userService.register(request));
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<TokenVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(userService.login(request));
    }

    @Operation(summary = "重新认证")
    @PostMapping("/refresh")
    public Result<TokenVO> refresh(@Valid @RequestBody RefreshRequest request) {
        return Result.success(userService.refresh(request));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(@Valid @RequestBody LogoutRequest request) {
        userService.logout(request);
        return Result.success();
    }

    @Operation(summary = "修改个人信息")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/updateProfile")
    public Result<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        userService.updateProfile(SecurityUtils.getUserId(), request);
        return Result.success();
    }

    @Operation(summary = "查询个人信息")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/getProfile")
    public Result<UserVO> getProfile() {
        return Result.success(userService.getProfile(SecurityUtils.getUserId()));
    }
}
