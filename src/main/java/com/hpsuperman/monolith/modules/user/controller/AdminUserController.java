package com.hpsuperman.monolith.modules.user.controller;

import com.hpsuperman.monolith.common.result.PageResult;
import com.hpsuperman.monolith.common.result.Result;
import com.hpsuperman.monolith.common.security.SecurityUtils;
import com.hpsuperman.monolith.modules.user.dto.AdminUserQuery;
import com.hpsuperman.monolith.modules.user.dto.AdminUserVO;
import com.hpsuperman.monolith.modules.user.dto.CreateUserRequest;
import com.hpsuperman.monolith.modules.user.dto.UpdateUserRequest;
import com.hpsuperman.monolith.modules.user.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "用户列表")
    @GetMapping
    public Result<PageResult<AdminUserVO>> list(@Valid AdminUserQuery query) {
        return Result.success(adminUserService.list(query));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    public Result<AdminUserVO> get(@PathVariable Long id) {
        return Result.success(adminUserService.get(id));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    public Result<AdminUserVO> create(@Valid @RequestBody CreateUserRequest request) {
        return Result.success(adminUserService.create(request));
    }

    @Operation(summary = "修改用户")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        adminUserService.update(SecurityUtils.getUserId(), id, request);
        return Result.success();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminUserService.delete(SecurityUtils.getUserId(), id);
        return Result.success();
    }

}
