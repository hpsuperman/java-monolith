package com.hpsuperman.monolith.modules.user.controller;

import com.hpsuperman.monolith.common.result.PageResult;
import com.hpsuperman.monolith.common.result.Result;
import com.hpsuperman.monolith.modules.user.dto.UserCreateRequest;
import com.hpsuperman.monolith.modules.user.dto.UserQueryRequest;
import com.hpsuperman.monolith.modules.user.dto.UserUpdateRequest;
import com.hpsuperman.monolith.modules.user.dto.UserVO;
import com.hpsuperman.monolith.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/users")
@Validated
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @Operation(summary = "分页查询用户")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<UserVO>> page(@Valid UserQueryRequest query) {
        return Result.success(userService.page(query));
    }

    @Operation(summary = "查询用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserVO> detail(@PathVariable @Positive(message = "用户 ID 必须为正数") Long id) {
        return Result.success(userService.getDetail(id));
    }

    @Operation(summary = "创建用户")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserVO> create(@Valid @RequestBody UserCreateRequest request) {
        return Result.success(userService.create(request));
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserVO> update(@PathVariable @Positive(message = "用户 ID 必须为正数") Long id,
                                 @Valid @RequestBody UserUpdateRequest request) {
        return Result.success(userService.update(id, request));
    }

    @Operation(summary = "删除用户（逻辑删除）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable @Positive(message = "用户 ID 必须为正数") Long id) {
        userService.delete(id);
        return Result.success();
    }
}
